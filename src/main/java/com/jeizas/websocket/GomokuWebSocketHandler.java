package com.jeizas.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeizas.model.GameMessage;
import com.jeizas.model.GameRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 五子棋WebSocket处理器，处理游戏相关的WebSocket连接和消息
 *
 * @author jeizas
 * @date 2025-11-29
 */
@Slf4j
public class GomokuWebSocketHandler extends TextWebSocketHandler {

    /** JSON对象映射器 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 存储所有游戏房间 */
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    /** 存储session到房间的映射 */
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    /** 默认房间ID */
    private static final String DEFAULT_ROOM = "default";

    /**
     * WebSocket连接建立后的处理
     *
     * @param session WebSocket会话
     * @throws Exception 处理异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("新玩家连接: {}", session.getId());
        joinRoom(session, DEFAULT_ROOM);
    }

    /**
     * 处理接收到的文本消息
     *
     * @param session WebSocket会话
     * @param message 文本消息
     * @throws Exception 处理异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到消息: {} from {}", payload, session.getId());

        GameMessage gameMessage = objectMapper.readValue(payload, GameMessage.class);
        String roomId = sessionToRoom.get(session.getId());

        if (roomId == null) {
            sendMessage(session, GameMessage.error("您不在任何房间中"));
            return;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            sendMessage(session, GameMessage.error("房间不存在"));
            return;
        }

        switch (gameMessage.getType()) {
            case "MOVE":
                handleMove(session, room, gameMessage);
                break;
            case "RESET":
                handleReset(room);
                break;
            default:
                log.warn("未知消息类型: {}", gameMessage.getType());
        }
    }

    /**
     * WebSocket连接关闭后的处理
     *
     * @param session WebSocket会话
     * @param status 关闭状态
     * @throws Exception 处理异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("玩家断开连接: {}", session.getId());
        leaveRoom(session);
    }

    /**
     * 玩家加入房间
     *
     * @param session WebSocket会话
     * @param roomId 房间ID
     * @throws IOException IO异常
     */
    private void joinRoom(WebSocketSession session,
                String roomId) throws IOException {
        GameRoom room = rooms.computeIfAbsent(roomId, GameRoom::new);

        synchronized (room) {
            if (room.getBlackPlayer() == null) {
                room.setBlackPlayer(session);
                sessionToRoom.put(session.getId(), roomId);
                log.info("玩家 {} 加入房间 {} 作为黑棋", session.getId(), roomId);
                sendMessage(session, GameMessage.waiting());
            } else if (room.getWhitePlayer() == null) {
                room.setWhitePlayer(session);
                sessionToRoom.put(session.getId(), roomId);
                room.setGameStarted(true);
                log.info("玩家 {} 加入房间 {} 作为白棋，游戏开始",
                        session.getId(), roomId);

                sendMessage(room.getBlackPlayer(), GameMessage.gameStart(1));
                sendMessage(room.getWhitePlayer(), GameMessage.gameStart(2));
            } else {
                sendMessage(session, GameMessage.error("房间已满，请稍后再试"));
                session.close();
            }
        }
    }

    /**
     * 玩家离开房间
     *
     * @param session WebSocket会话
     * @throws IOException IO异常
     */
    private void leaveRoom(WebSocketSession session) throws IOException {
        String roomId = sessionToRoom.remove(session.getId());
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (room) {
            WebSocketSession opponent = room.getOpponent(session);

            if (session.equals(room.getBlackPlayer())) {
                room.setBlackPlayer(null);
            } else if (session.equals(room.getWhitePlayer())) {
                room.setWhitePlayer(null);
            }

            if (opponent != null && opponent.isOpen()) {
                sendMessage(opponent, GameMessage.opponentLeft());
            }

            room.reset();

            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    /**
     * 处理玩家落子
     *
     * @param session WebSocket会话
     * @param room 游戏房间
     * @param message 游戏消息
     * @throws IOException IO异常
     */
    private void handleMove(WebSocketSession session, GameRoom room, GameMessage message) throws IOException {
        synchronized (room) {
            if (!room.isGameStarted()) {
                sendMessage(session, GameMessage.error("游戏尚未开始"));
                return;
            }

            if (room.isGameOver()) {
                sendMessage(session, GameMessage.error("游戏已结束"));
                return;
            }

            int playerColor = room.getPlayerColor(session);
            if (playerColor != room.getCurrentPlayer()) {
                sendMessage(session, GameMessage.error("还没轮到你"));
                return;
            }

            int row = message.getRow();
            int col = message.getCol();

            if (row < 0 || row >= 15 || col < 0 || col >= 15) {
                sendMessage(session, GameMessage.error("无效的位置"));
                return;
            }

            if (room.getBoard()[row][col] != 0) {
                sendMessage(session, GameMessage.error("该位置已有棋子"));
                return;
            }

            room.getBoard()[row][col] = playerColor;

            boolean win = room.checkWin(row, col, playerColor);

            if (win) {
                room.setGameOver(true);
                room.setWinner(playerColor);

                GameMessage moveMsg = GameMessage.move(row, col, playerColor, 0, room.getBoard());
                sendMessage(room.getBlackPlayer(), moveMsg);
                sendMessage(room.getWhitePlayer(), moveMsg);

                GameMessage gameOverMsg = GameMessage.gameOver(playerColor);
                sendMessage(room.getBlackPlayer(), gameOverMsg);
                sendMessage(room.getWhitePlayer(), gameOverMsg);
            } else {
                room.setCurrentPlayer(playerColor == 1 ? 2 : 1);

                GameMessage moveMsg = GameMessage.move(row, col, playerColor, room.getCurrentPlayer(), room.getBoard());
                sendMessage(room.getBlackPlayer(), moveMsg);
                sendMessage(room.getWhitePlayer(), moveMsg);
            }
        }
    }

    /**
     * 处理游戏重置
     *
     * @param room 游戏房间
     * @throws IOException IO异常
     */
    private void handleReset(GameRoom room) throws IOException {
        synchronized (room) {
            room.reset();
            room.setGameStarted(true);

            if (room.getBlackPlayer() != null && room.getBlackPlayer().isOpen()) {
                sendMessage(room.getBlackPlayer(), GameMessage.reset());
                sendMessage(room.getBlackPlayer(), GameMessage.gameStart(1));
            }
            if (room.getWhitePlayer() != null && room.getWhitePlayer().isOpen()) {
                sendMessage(room.getWhitePlayer(), GameMessage.reset());
                sendMessage(room.getWhitePlayer(), GameMessage.gameStart(2));
            }
        }
    }

    /**
     * 发送消息到指定会话
     *
     * @param session WebSocket会话
     * @param message 游戏消息
     * @throws IOException IO异常
     */
    private void sendMessage(WebSocketSession session, GameMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("发送消息到 {}: {}", session.getId(), json);
        }
    }
}

