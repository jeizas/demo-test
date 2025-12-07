package com.jeizas.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeizas.model.mahjong.MahjongMessage;
import com.jeizas.model.mahjong.MahjongPlayer;
import com.jeizas.model.mahjong.MahjongRoom;
import com.jeizas.model.mahjong.MahjongTile;
import com.jeizas.service.MahjongBotService;
import com.jeizas.service.MahjongGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 麻将WebSocket处理器
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Slf4j
public class MahjongWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private MahjongGameService gameService;

    @Autowired
    private MahjongBotService botService;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MahjongRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayer = new ConcurrentHashMap<>();
    private static final String DEFAULT_ROOM = "default";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("新玩家连接: {}", session.getId());
        String playerId = UUID.randomUUID().toString();
        sessionToPlayer.put(session.getId(), playerId);

        MahjongRoom room = rooms.computeIfAbsent(DEFAULT_ROOM, MahjongRoom::new);
        synchronized (room.getLock()) {
            MahjongPlayer player = new MahjongPlayer(playerId, "玩家" + (room.getPlayers().size() + 1),
                    false, session);
            if (room.addPlayer(player)) {
                sessionToRoom.put(session.getId(), DEFAULT_ROOM);
                log.info("玩家 {} 加入房间 {}", playerId, DEFAULT_ROOM);

                // 发送房间状态更新
                broadcastRoomUpdate(room);
            } else {
                sendMessage(session, MahjongMessage.error("房间已满"));
                session.close();
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到消息: {} from {}", payload, session.getId());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String type = (String) data.get("type");
        String roomId = sessionToRoom.get(session.getId());
        String playerId = sessionToPlayer.get(session.getId());

        if (roomId == null || playerId == null) {
            sendMessage(session, MahjongMessage.error("未加入房间"));
            return;
        }

        MahjongRoom room = rooms.get(roomId);
        if (room == null) {
            sendMessage(session, MahjongMessage.error("房间不存在"));
            return;
        }

        synchronized (room.getLock()) {
            switch (type) {
                case "ADD_BOT":
                    handleAddBot(room, data);
                    break;
                case "SET_ROUNDS":
                    handleSetRounds(room, data);
                    break;
                case "START_GAME":
                    handleStartGame(room);
                    break;
                case "DISCARD_TILE":
                    handleDiscardTile(room, playerId, data);
                    break;
                case "WIN":
                    handleWin(room, playerId);
                    break;
                case "NEXT_ROUND":
                    handleNextRound(room);
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("玩家断开连接: {}", session.getId());
        String roomId = sessionToRoom.remove(session.getId());
        String playerId = sessionToPlayer.remove(session.getId());

        if (roomId != null && playerId != null) {
            MahjongRoom room = rooms.get(roomId);
            if (room != null) {
                synchronized (room.getLock()) {
                    room.removePlayer(playerId);
                    if (room.isEmpty()) {
                        rooms.remove(roomId);
                    } else {
                        broadcastRoomUpdate(room);
                        if (room.isGameStarted()) {
                            broadcastMessage(room, MahjongMessage.playerLeft("玩家离开，游戏结束"));
                            room.reset();
                        }
                    }
                }
            }
        }
    }

    private void handleAddBot(MahjongRoom room, Map<String, Object> data) throws IOException {
        if (room.isGameStarted()) {
            return;
        }

        if (room.isFull()) {
            return;
        }

        String botId = UUID.randomUUID().toString();
        MahjongPlayer bot = new MahjongPlayer(botId, "机器人" + (room.getPlayers().size() + 1),
                true, null);
        room.addPlayer(bot);
        log.info("添加机器人: {}", bot.getPlayerName());

        broadcastRoomUpdate(room);
    }

    private void handleSetRounds(MahjongRoom room, Map<String, Object> data) throws IOException {
        if (room.isGameStarted()) {
            return;
        }

        int rounds = (int) data.get("rounds");
        room.setTotalRounds(rounds);
        log.info("设置轮数: {}", rounds);

        broadcastRoomUpdate(room);
    }

    private void handleStartGame(MahjongRoom room) throws IOException {
        if (!room.isFull()) {
            return;
        }

        // 所有玩家设置为准备
        for (MahjongPlayer player : room.getPlayers()) {
            player.setReady(true);
        }

        room.setCurrentRound(1);
        room.startNewGame();
        log.info("游戏开始");

        // 发送游戏开始消息
        for (MahjongPlayer player : room.getPlayers()) {
            List<Map<String, Object>> playersInfo = getPlayersInfo(room);
            MahjongMessage msg = MahjongMessage.gameStart(
                    player.getHand(),
                    playersInfo,
                    room.getDealerIndex(),
                    room.getTotalRounds()
            );
            sendMessageToPlayer(player, msg);
        }

        // 如果当前玩家是机器人，触发机器人行动
        if (room.getCurrentPlayer().isBot()) {
            scheduleBotAction(room);
        }
    }

    private void handleDiscardTile(MahjongRoom room, String playerId, Map<String, Object> data)
            throws IOException {
        if (!room.isGameStarted() || room.isGameOver()) {
            return;
        }

        MahjongPlayer player = room.getPlayer(playerId);
        if (player == null) {
            return;
        }

        if (room.getCurrentPlayer() != player) {
            sendMessageToPlayer(player, MahjongMessage.error("还没轮到你"));
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> tileData = (Map<String, Object>) data.get("tile");
        String typeStr = (String) tileData.get("type");
        int value = (int) tileData.get("value");
        MahjongTile.TileType type = MahjongTile.TileType.valueOf(typeStr);
        MahjongTile tile = new MahjongTile(type, value);

        player.discardTile(tile);
        room.setLastDiscardedTile(tile);
        room.setLastDiscardPlayerIndex(player.getPosition());

        // 检查其他玩家是否能胡、碰、杠
        boolean someoneCanWin = false;
        for (MahjongPlayer otherPlayer : room.getPlayers()) {
            if (otherPlayer != player) {
                // 检查是否能胡
                List<MahjongTile> testHand = new ArrayList<>(otherPlayer.getHand());
                testHand.add(tile);
                if (gameService.canWin(testHand)) {
                    someoneCanWin = true;
                    // 如果是机器人且可以胡，自动胡牌
                    if (otherPlayer.isBot()) {
                        handleWinByDiscard(room, otherPlayer, player, tile);
                        return;
                    }
                }

                // 检查是否能碰或杠（非机器人才提示）
                if (!otherPlayer.isBot()) {
                    boolean canPeng = otherPlayer.canPeng(tile);
                    boolean canGang = otherPlayer.canMingGang(tile);

                    if (canPeng || canGang) {
                        sendMessageToPlayer(otherPlayer,
                            MahjongMessage.canMeld(tile, canPeng, canGang));
                    }
                }
            }
        }

        if (!someoneCanWin) {
            // 下一个玩家
            room.nextPlayer();
            MahjongPlayer nextPlayer = room.getCurrentPlayer();

            // 摸牌
            MahjongTile drawnTile = room.drawTile();
            if (drawnTile == null) {
                // 流局
                handleDraw(room);
                return;
            }

            nextPlayer.addTile(drawnTile);

            // 广播打牌消息
            broadcastMessage(room, MahjongMessage.discardTile(
                    playerId, tile, room.getCurrentPlayerIndex(), room.getDeck().size()
            ));

            // 检查是否自摸
            boolean canWin = gameService.canWin(nextPlayer.getHand());

            // 发送摸牌消息
            if (!nextPlayer.isBot()) {
                sendMessageToPlayer(nextPlayer, MahjongMessage.drawTile(drawnTile, canWin));
            }

            if (canWin) {
                if (nextPlayer.isBot()) {
                    // 机器人自动胡牌
                    scheduler.schedule(() -> {
                        try {
                            synchronized (room.getLock()) {
                                handleWinBySelfDraw(room, nextPlayer, drawnTile);
                            }
                        } catch (IOException e) {
                            log.error("机器人胡牌失败", e);
                        }
                    }, botService.getThinkingDelay(), TimeUnit.MILLISECONDS);
                }
            } else if (nextPlayer.isBot()) {
                // 机器人打牌
                scheduleBotAction(room);
            }
        }
    }

    private void handleWin(MahjongRoom room, String playerId) throws IOException {
        MahjongPlayer player = room.getPlayer(playerId);
        if (player == null || room.getLastDiscardedTile() == null) {
            return;
        }

        MahjongPlayer discardPlayer = room.getPlayers().get(room.getLastDiscardPlayerIndex());
        handleWinByDiscard(room, player, discardPlayer, room.getLastDiscardedTile());
    }

    private void handleWinByDiscard(MahjongRoom room, MahjongPlayer winner,
                                     MahjongPlayer discardPlayer, MahjongTile tile) throws IOException {
        winner.addTile(tile);

        int score = gameService.calculateScore(winner.getHand(), false);
        winner.setScore(winner.getScore() + score);
        discardPlayer.setScore(discardPlayer.getScore() - score);

        Map<String, Object> winInfo = new HashMap<>();
        winInfo.put("winnerId", winner.getPlayerId());
        winInfo.put("winnerName", winner.getPlayerName());
        winInfo.put("discardPlayerId", discardPlayer.getPlayerId());
        winInfo.put("discardPlayerName", discardPlayer.getPlayerName());
        winInfo.put("score", score);
        winInfo.put("tile", tile);

        broadcastMessage(room, MahjongMessage.win(winner.getPlayerId(), winInfo));

        room.endCurrentGame();

        if (room.isGameOver()) {
            handleGameOver(room);
        }
    }

    private void handleWinBySelfDraw(MahjongRoom room, MahjongPlayer winner, MahjongTile tile)
            throws IOException {
        int score = gameService.calculateScore(winner.getHand(), true);
        winner.setScore(winner.getScore() + score * 3);

        for (MahjongPlayer player : room.getPlayers()) {
            if (player != winner) {
                player.setScore(player.getScore() - score);
            }
        }

        Map<String, Object> winInfo = new HashMap<>();
        winInfo.put("winnerId", winner.getPlayerId());
        winInfo.put("winnerName", winner.getPlayerName());
        winInfo.put("isSelfDraw", true);
        winInfo.put("score", score);
        winInfo.put("tile", tile);

        broadcastMessage(room, MahjongMessage.win(winner.getPlayerId(), winInfo));

        room.endCurrentGame();

        if (room.isGameOver()) {
            handleGameOver(room);
        }
    }

    private void handleDraw(MahjongRoom room) throws IOException {
        broadcastMessage(room, MahjongMessage.error("流局"));
        room.endCurrentGame();

        if (room.isGameOver()) {
            handleGameOver(room);
        }
    }

    private void handleGameOver(MahjongRoom room) throws IOException {
        List<Map<String, Object>> scores = new ArrayList<>();
        for (MahjongPlayer player : room.getPlayers()) {
            Map<String, Object> playerScore = new HashMap<>();
            playerScore.put("playerId", player.getPlayerId());
            playerScore.put("playerName", player.getPlayerName());
            playerScore.put("score", player.getScore());
            playerScore.put("isBot", player.isBot());
            scores.add(playerScore);
        }

        scores.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        broadcastMessage(room, MahjongMessage.gameOver(scores));
    }

    private void handleNextRound(MahjongRoom room) throws IOException {
        if (room.isGameOver()) {
            room.reset();
            room.setCurrentRound(1);
        }

        room.startNewGame();

        for (MahjongPlayer player : room.getPlayers()) {
            List<Map<String, Object>> playersInfo = getPlayersInfo(room);
            MahjongMessage msg = MahjongMessage.gameStart(
                    player.getHand(),
                    playersInfo,
                    room.getDealerIndex(),
                    room.getTotalRounds()
            );
            sendMessageToPlayer(player, msg);
        }

        if (room.getCurrentPlayer().isBot()) {
            scheduleBotAction(room);
        }
    }

    private void scheduleBotAction(MahjongRoom room) {
        scheduler.schedule(() -> {
            try {
                synchronized (room.getLock()) {
                    if (!room.isGameStarted() || room.isGameOver()) {
                        return;
                    }

                    MahjongPlayer bot = room.getCurrentPlayer();
                    if (!bot.isBot()) {
                        return;
                    }

                    MahjongTile tileToDiscard = botService.chooseTileToDiscard(bot,
                            bot.getHand().get(bot.getHand().size() - 1));

                    if (tileToDiscard != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("type", "DISCARD_TILE");
                        Map<String, Object> tileData = new HashMap<>();
                        tileData.put("type", tileToDiscard.getType().name());
                        tileData.put("value", tileToDiscard.getValue());
                        data.put("tile", tileData);

                        handleDiscardTile(room, bot.getPlayerId(), data);
                    }
                }
            } catch (Exception e) {
                log.error("机器人行动失败", e);
            }
        }, botService.getThinkingDelay(), TimeUnit.MILLISECONDS);
    }

    private List<Map<String, Object>> getPlayersInfo(MahjongRoom room) {
        List<Map<String, Object>> playersInfo = new ArrayList<>();
        for (MahjongPlayer p : room.getPlayers()) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("playerId", p.getPlayerId());
            playerInfo.put("playerName", p.getPlayerName());
            playerInfo.put("isBot", p.isBot());
            playerInfo.put("position", p.getPosition());
            playerInfo.put("isDealer", p.isDealer());
            playerInfo.put("score", p.getScore());
            playerInfo.put("handCount", p.getHand().size());
            playerInfo.put("discardedTiles", p.getDiscardedTiles());
            playersInfo.add(playerInfo);
        }
        return playersInfo;
    }

    private void broadcastRoomUpdate(MahjongRoom room) throws IOException {
        List<Map<String, Object>> playersInfo = getPlayersInfo(room);
        MahjongMessage msg = MahjongMessage.roomUpdate(playersInfo);
        broadcastMessage(room, msg);
    }

    private void broadcastMessage(MahjongRoom room, MahjongMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        for (MahjongPlayer player : room.getPlayers()) {
            if (!player.isBot() && player.getSession() != null && player.getSession().isOpen()) {
                player.getSession().sendMessage(new TextMessage(json));
            }
        }
    }

    private void sendMessageToPlayer(MahjongPlayer player, MahjongMessage message) throws IOException {
        if (!player.isBot() && player.getSession() != null && player.getSession().isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            player.getSession().sendMessage(new TextMessage(json));
        }
    }

    private void sendMessage(WebSocketSession session, MahjongMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
}
