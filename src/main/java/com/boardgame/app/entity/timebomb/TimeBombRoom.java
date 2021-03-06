package com.boardgame.app.entity.timebomb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.boardgame.app.constclass.timebomb.TimeBombConst;
import com.boardgame.app.entity.Room;
import com.boardgame.app.entity.User;
import com.boardgame.app.entity.enif.LimitTimeInterface;
import com.boardgame.app.exception.ApplicationException;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimeBombRoom extends Room implements LimitTimeInterface {
	private static final long serialVersionUID = 6252639126815391708L;
	private int turn;
	private List<LeadCards> leadCardsList;
	private int winnerTeam;
	private int round;
	private int releaseNo;
	private int limitTime;
	private boolean secretFlg;

	public TimeBombRoom() {
		maxUserSize = TimeBombConst.DEFAULT_MAXUSERSIZE;
		userList = new ArrayList<User>();
		turn = 0;
		winnerTeam = 0;
		round = 0;
		releaseNo = 0;
		secretFlg = false;
	}

	public void init() {
		// ターンの初期化
		turn = 1;
		round = 1;
		winnerTeam = 0;
		releaseNo = 0;

		// ユーザの初期化
		int userSize = userList.size();
		maxUserSize = userSize;

		// 役職取得
		List<Integer> userTypeList = TimeBombConst.getUserList(userSize);

		// 手番ユーザの乱数生成
		Random rand = new Random();
		int turnUserNo = rand.nextInt(userSize);

		for (int i = 0; i < userSize; i++) {
			TimeBombUser timeBombUser = (TimeBombUser) userList.get(i);

			timeBombUser.setUserNo(i + 1);

			timeBombUser.setUserRoleNo(userTypeList.get(i));

			if (turnUserNo == i) {
				timeBombUser.setTurnFlg(true);
			} else {
				timeBombUser.setTurnFlg(false);
			}
		}

		// カードの初期化
		List<Integer> cardsTypeList = TimeBombConst.getCardsList(userSize);
		leadCardsList = new ArrayList<LeadCards>();
		for (Integer integer : cardsTypeList) {
			leadCardsList.add(new LeadCards(integer));
		}

	}

	@Override
	public User joinUser(String userName) throws ApplicationException {
		User user = new TimeBombUser();
		user.setUserName(userName);
		addUser(user);
		return user;
	}

	public void playTurn(int cardIndex) throws ApplicationException {
		// エラーチェック
		if (leadCardsList.get(cardIndex).isOpenFlg()) {
			throw new ApplicationException("対象カードなし");
		}

		// 実行
		leadCardsList.get(cardIndex).setOpenFlg(true);

		// ターン経過
		turn++;

		// 解除数のカウント
		if (leadCardsList.get(cardIndex).getCardType() == TimeBombConst.RELEASE_CARD_NO) {
			releaseNo++;
		}

		// 手番ユーザの決定
		int turnUserNo = (cardIndex) / (6 - round);

		for (int i = 0; i < userList.size(); i++) {
			TimeBombUser timeBombUser = (TimeBombUser) userList.get(i);

			if (turnUserNo == i) {
				timeBombUser.setTurnFlg(true);
			} else {
				timeBombUser.setTurnFlg(false);
			}
		}

		judgment();

		if (winnerTeam > 0) {
			// 勝敗が決定したためカードを全て表にする
			if (winnerTeam > 0) {
				leadCardsList.forEach(o -> o.setOpenFlg(true));
			}
			return;
		}

		// ラウンドチェック
		if ((turn - 1) % userList.size() == 0) {
			round++;

			// カードの回収
			leadCardsList = leadCardsList.stream().filter(o -> !o.isOpenFlg()).collect(Collectors.toList());
			// 入れ替える
			Collections.shuffle(leadCardsList);

		}

	}

	/**
	 * 0:勝敗なし
	 */
	private void judgment() {
		int result = 0;

		if (leadCardsList.stream().filter(o -> (o.getCardType() == TimeBombConst.BOMB_CARD_NO) && o.isOpenFlg())
				.count() > 0) {
			// 爆弾が爆発した場合
			result = TimeBombConst.BOMB_TEAM;
		} else if (releaseNo >= userList.size()) {
			// 解除成功
			result = TimeBombConst.TIME_POLIS;
		} else if (turn > userList.size() * 4) {
			// 時間切れ
			result = TimeBombConst.BOMB_TEAM;
		}

		winnerTeam = result;

	}

	@Override
	public void doOverLimit(int turn) throws ApplicationException {
		if (turn != this.turn || winnerTeam > 0) {
			// 処理終了
			return;
		}

		try {
			// 開いていないカード取得

			TimeBombUser turnUser = null;
			for (User user : userList) {
				TimeBombUser timebombUser = (TimeBombUser) user;

				if (timebombUser.isTurnFlg()) {
					turnUser = timebombUser;
				}
			}

			int index = 0;
			for (int i = 0; i < leadCardsList.size(); i++) {
				if (!leadCardsList.get(i).isOpenFlg()) {
					if ((6 - round) * (turnUser.getUserNo() - 1) <= i && i < (6 - round) * turnUser.getUserNo()) {
						// 自分のカードであるため継続
						continue;
					}
					index = i;
					break;
				}
			}

			playTurn(index);

		} catch (Throwable e) {
			throw new ApplicationException(e.getMessage());
		}
	}

}
