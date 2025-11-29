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

@Slf4j
public class GomokuWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储所有游戏房间
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    
    // 存储 session 到房间的映射
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    
    // 默认房间ID（简化版，所有人加入同一房间）
    private static final String DEFAULT_ROOM = "default";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("新玩家连接: {}", session.getId());
        joinRoom(session, DEFAULT_ROOM);
    }

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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("玩家断开连接: {}", session.getId());
        leaveRoom(session);
    }

    private void joinRoom(WebSocketSession session, String roomId) throws IOException {
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
                log.info("玩家 {} 加入房间 {} 作为白棋，游戏开始", session.getId(), roomId);
                
                // 通知双方游戏开始
                sendMessage(room.getBlackPlayer(), GameMessage.gameStart(1));
                sendMessage(room.getWhitePlayer(), GameMessage.gameStart(2));
            } else {
                // 房间已满，创建新房间或拒绝
                sendMessage(session, GameMessage.error("房间已满，请稍后再试"));
                session.close();
            }
        }
    }

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
            
            // 通知对手
            if (opponent != null && opponent.isOpen()) {
                sendMessage(opponent, GameMessage.opponentLeft());
            }
            
            // 重置房间状态
            room.reset();
            
            // 如果房间为空，删除房间
            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

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
            
            // 检查位置是否有效
            if (row < 0 || row >= 15 || col < 0 || col >= 15) {
                sendMessage(session, GameMessage.error("无效的位置"));
                return;
            }
            
            if (room.getBoard()[row][col] != 0) {
                sendMessage(session, GameMessage.error("该位置已有棋子"));
                return;
            }
            
            // 落子
            room.getBoard()[row][col] = playerColor;
            
            // 检查胜负
            boolean win = room.checkWin(row, col, playerColor);
            
            if (win) {
                room.setGameOver(true);
                room.setWinner(playerColor);
                
                // 发送落子消息
                GameMessage moveMsg = GameMessage.move(row, col, playerColor, 0, room.getBoard());
                sendMessage(room.getBlackPlayer(), moveMsg);
                sendMessage(room.getWhitePlayer(), moveMsg);
                
                // 发送游戏结束消息
                GameMessage gameOverMsg = GameMessage.gameOver(playerColor);
                sendMessage(room.getBlackPlayer(), gameOverMsg);
                sendMessage(room.getWhitePlayer(), gameOverMsg);
            } else {
                // 切换玩家
                room.setCurrentPlayer(playerColor == 1 ? 2 : 1);
                
                // 通知双方落子结果
                GameMessage moveMsg = GameMessage.move(row, col, playerColor, room.getCurrentPlayer(), room.getBoard());
                sendMessage(room.getBlackPlayer(), moveMsg);
                sendMessage(room.getWhitePlayer(), moveMsg);
            }
        }
    }

    private void handleReset(GameRoom room) throws IOException {
        synchronized (room) {
            room.reset();
            room.setGameStarted(true);
            
            // 通知双方游戏重置
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

    private void sendMessage(WebSocketSession session, GameMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("发送消息到 {}: {}", session.getId(), json);
        }
    }
}

