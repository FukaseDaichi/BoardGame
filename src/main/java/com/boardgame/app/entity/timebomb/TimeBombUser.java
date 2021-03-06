package com.boardgame.app.entity.timebomb;

import com.boardgame.app.entity.User;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimeBombUser extends User {
	private static final long serialVersionUID = -608311496580970104L;
	// 1から始まる
	private int userRoleNo;

	private boolean turnFlg;
}
