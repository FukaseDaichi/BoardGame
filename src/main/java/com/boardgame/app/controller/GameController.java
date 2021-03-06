package com.boardgame.app.controller;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.boardgame.app.component.ApplicationInfoBeean;
import com.boardgame.app.controller.common.CommonLogic;
import com.boardgame.app.entity.ErrObj;
import com.boardgame.app.entity.Room;
import com.boardgame.app.entity.SocketInfo;
import com.boardgame.app.entity.User;
import com.boardgame.app.entity.chat.ChatRoom;
import com.boardgame.app.entity.enif.LimitTimeInterface;
import com.boardgame.app.exception.ApplicationException;

@Controller
public class GameController {

	@Autowired
	SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private ApplicationInfoBeean appInfo;

	@MessageMapping("game-roomin")
	public void gameRoomIn(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();

		try {
			Room room = appInfo.getRoom(socketInfo.getRoomId());
			SocketInfo obj = null;
			if (room != null) {
				try {
					room.joinUser(socketInfo.getUserName());

				} catch (ApplicationException e) {
					obj = new SocketInfo(e.getStatus(), e.getMessage(), room);
					obj.setUserName(socketInfo.getUserName());
					simpMessagingTemplate.convertAndSend(description, obj);
					return;
				}
			} else {
				obj = new SocketInfo(HttpsURLConnection.HTTP_NOT_FOUND, "部屋が存在しません。部屋の作成をしてください", null);
				simpMessagingTemplate.convertAndSend(description, obj);
				return;
			}
			socketInfo.setObj(room);
			simpMessagingTemplate.convertAndSend(description, socketInfo);
		} catch (Throwable e) {
			simpMessagingTemplate.convertAndSend(description,
					new SocketInfo(HttpsURLConnection.HTTP_NOT_FOUND, "部屋が存在しません。部屋の作成をしてください", null));

		}
	}

	@MessageMapping("game-removeuser")
	public void gameRemoveUser(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();
		Room room = appInfo.getRoom(socketInfo.getRoomId());
		if (CommonLogic.isExistRoom(appInfo, simpMessagingTemplate, socketInfo)) {
			room.removeUser((String) socketInfo.getObj());
			socketInfo.setObj(room);
			simpMessagingTemplate.convertAndSend(description, socketInfo);
		}

	}

	@MessageMapping("game-chat")
	public void chat(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();
		ChatRoom room = (ChatRoom) appInfo.getRoom(socketInfo.getRoomId());
		User user = room.getUserbyName(socketInfo.getUserName());
		room.chat(user, socketInfo.getMessage());

		SocketInfo rtnObj = new SocketInfo(socketInfo.getStatus(), null, room.getChatList());

		simpMessagingTemplate.convertAndSend(description, rtnObj);

	}

	@MessageMapping("game-setlimittime")
	public void setLimitTime(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();
		LimitTimeInterface room = (LimitTimeInterface) appInfo.getRoom(socketInfo.getRoomId());

		room.setLimitTime((Integer) socketInfo.getObj());

		SocketInfo rtnObj = new SocketInfo(socketInfo.getStatus(), null, room.getLimitTime());

		simpMessagingTemplate.convertAndSend(description, rtnObj);

	}

	@MessageMapping("game-dooverLimit")
	public void doOverLimit(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();
		LimitTimeInterface room = (LimitTimeInterface) appInfo.getRoom(socketInfo.getRoomId());

		room.doOverLimit((Integer) socketInfo.getObj());
		socketInfo.setObj(room);
		simpMessagingTemplate.convertAndSend(description, socketInfo);
	}

	@MessageMapping("game-changeIcon")
	public void changeIcon(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();

		Room room = appInfo.getRoom(socketInfo.getRoomId());

		if (room != null) {
			room.getUserList().forEach(o -> {
				if (o.getUserName().equals(socketInfo.getUserName())) {
					o.setUserIconUrl((String) socketInfo.getObj());
				}
			});
		} else {
			ErrObj obj = new ErrObj(HttpsURLConnection.HTTP_NOT_FOUND, "部屋が存在しません。部屋の作成をしてください", null);
			simpMessagingTemplate.convertAndSend(description, obj);
			return;
		}
		SocketInfo rtnObj = new SocketInfo(socketInfo.getStatus(), null, room.getUserList());

		simpMessagingTemplate.convertAndSend(description, rtnObj);
	}

	@MessageMapping("game-action")
	public void gameAction(SocketInfo socketInfo) throws Exception {
		String description = "/topic/" + socketInfo.getRoomId();
		if (CommonLogic.isExistRoom(appInfo, simpMessagingTemplate, socketInfo)) {
			simpMessagingTemplate.convertAndSend(description, socketInfo);
		}
	}

}