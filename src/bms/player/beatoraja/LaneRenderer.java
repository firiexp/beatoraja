package bms.player.beatoraja;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import bms.model.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * レーン描画用クラス
 * 
 * @author exch
 */
public class LaneRenderer {

	/**
	 * レーンカバーの量
	 */
	private float lanecover = 0.2f;
	/**
	 * レーンカバーを表示するかどうか
	 */
	private boolean enableLanecover = true;

	private long lanecovertiming;
	/**
	 * リフトの量
	 */
	private float lift = 0.05f;
	/**
	 * リフトを使用するかどうか
	 */
	private boolean enableLift = true;

	private float hispeed = 1.0f;

	private int gvalue;

	private boolean fixhispeed;
	private float basehispeed;
	private boolean bpmguide = true;

	private BMSModel model;

	private int pos;

	private BMSPlayer main;

	private SpriteBatch sprite;
	private PlaySkin skin;

	Color[] lanebg = { new Color(0.1f, 0.1f, 0.1f, 1), Color.BLACK, new Color(0.1f, 0.1f, 0.1f, 1), Color.BLACK,
			new Color(0.1f, 0.1f, 0.1f, 1), Color.BLACK, new Color(0.1f, 0.1f, 0.1f, 1), Color.BLACK };

	private int timing = 0;

	public LaneRenderer(BMSPlayer main, SpriteBatch sprite, PlaySkin skin, Config config, BMSModel model) {
		this.main = main;
		this.sprite = sprite;
		this.skin = skin;
		this.enableLanecover = config.isEnablelanecover();
		this.enableLift = config.isEnablelift();
		this.lift = config.getLift();
		this.timing = config.getJudgetiming();
		this.bpmguide = config.isBpmguide();
		this.fixhispeed = config.isFixhispeed();
		this.gvalue = config.getGreenvalue();
		this.model = model;
		hispeed = config.getHispeed();
		this.setLanecover(config.getLanecover());
		if (this.fixhispeed) {
			basehispeed = hispeed;
		}
	}

	public float getLanecover() {
		return lanecover;
	}

	public void setLanecover(float lanecover) {
		this.lanecover = lanecover;
		if (this.fixhispeed) {
			hispeed = (float) ((3000 / (model.getBpm() / 100) / gvalue) * 0.6
					* (1 - (enableLanecover ? lanecover : 0)));
		}
	}

	public void setEnableLanecover(boolean b) {
		enableLanecover = b;
	}

	public boolean isEnableLanecover() {
		return enableLanecover;
	}

	public void changeHispeed(boolean b) {
		float f = 0;
		if (fixhispeed) {
			f = basehispeed * 0.25f * (b ? 1 : -1);
		} else {
			f = 0.125f * (b ? 1 : -1);
		}
		if (hispeed + f > 0 && hispeed + f < 20) {
			hispeed += f;
		}
	}

	public void drawLane(ShapeRenderer shape, BitmapFont font, BMSModel model, TimeLine[] timelines, long starttime,
			long time) {
		time += timing;
		JudgeManager judge = main.getJudgeManager();
		final Rectangle[] laneregion = skin.getLaneregion();
		final float[] playerx = { laneregion[7].x };
		final float[] playerw = { laneregion[6].x + laneregion[6].width - laneregion[7].x };
		double bpm = model.getBpm();
		double nbpm = bpm;
		for (TimeLine t : timelines) {
			if (t.getTime() > time) {
				break;
			}
			if (t.getBPM() > 0) {
				bpm = t.getBPM();
			}
			nbpm = t.getBPM();
		}
		int region = (int) (3000 / (bpm / 100) / hispeed);
		float hu = laneregion[0].y + laneregion[0].height;
		float hl = laneregion[0].y;

		// リフト描画
		if (enableLift) {
			for (int p = 0; p < playerx.length; p++) {
				sprite.begin();
				sprite.draw(skin.getLanecover(), playerx[p], hl, playerw[p], (hu - hl) * lift);
				// 緑数字、白数字描画
				if (main.getBMSPlayerInputProcessor().startPressed()) {
					font.setColor(Color.WHITE);
					font.draw(sprite, String.format("%5d", Math.round(lift * 1000)), playerx[p] + playerw[p] * 0.25f,
							hl + (hu - hl) * lift);
					font.setColor(Color.GREEN);
					font.draw(sprite,
							String.format("%5d", Math.round(region * 0.6 * (1 - (enableLanecover ? lanecover : 0)))),
							playerx[p] + playerw[p] * 0.75f, hl + (hu - hl) * lift);
				}
				sprite.end();
			}
			hl = hl + (hu - hl) * lift;
		}
		// 判定ライン描画
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.RED);
		shape.rect(playerx[0], hl - 3, playerw[0], 6);

		shape.end();

		boolean[] keystate = main.getBMSPlayerInputProcessor().getKeystate();
		for (int lane = 0; lane < laneregion.length; lane++) {
			float x = laneregion[lane].x;
			float dx = laneregion[lane].width;
			shape.begin(ShapeType.Filled);
			shape.setColor(lanebg[lane]);
			shape.rect(x, hl, dx, hu - hl);

			shape.setColor(Color.GRAY);
			shape.line(x, hl, x, hu);
			shape.line(x + dx, hl, x + dx, hu);
			shape.end();

			// キービーム描画
			if (keystate[lane] || (lane == 7 && keystate[8])) {
				sprite.begin();
				if (lane == 7) {
					sprite.draw(skin.getKeybeam()[2], x, hl, dx, hu - hl);
				} else if (lane % 2 == 0) {
					sprite.draw(skin.getKeybeam()[0], x, hl, dx, hu - hl);
				} else {
					sprite.draw(skin.getKeybeam()[1], x, hl, dx, hu - hl);
				}
				sprite.end();
			}
		}
		// TODO ここでカバー移動判定を行うとオートプレイ時に誤反応するので、MusicSelectのスクラッチ移動判定と共にInputProcessorに集約したい
		// move lane cover by START + Scratch
		if (main.getBMSPlayerInputProcessor().startPressed() && (keystate[7] | keystate[8])) {
			long l = System.currentTimeMillis();
			if (l - lanecovertiming > 50) {
				float f = lanecover;
				f = f + (keystate[7] ? 0.001f : -0.001f);
				if (f > 1) {
					f = 1;
				}
				if (f < 0) {
					f = 0;
				}
				this.setLanecover(f);
				lanecovertiming = l;
			}
		}

		float y = hl;

		for (int i = pos; i < timelines.length && y <= hu; i++) {
			TimeLine tl = timelines[i];
			if (tl.getTime() >= time) {
				if (nbpm > 0) {
					if (i > 0 && timelines[i - 1].getTime() > time) {
						y += (float) (timelines[i].getTime() - timelines[i - 1].getTime()) * (hu - hl)
								/ ((float) region / nbpm * bpm);
					} else {
						y += (float) (timelines[i].getTime() - time) * (hu - hl) / ((float) region / nbpm * bpm);
					}
				}
				if (bpmguide && tl.getBPM() != nbpm) {
					// BPMガイド描画
					shape.begin(ShapeType.Line);
					for (int p = 0; p < playerx.length; p++) {
						shape.setColor(Color.valueOf("00c000"));
						shape.line(playerx[p], y + 2, playerx[p] + playerw[p], y + 2);
						shape.line(playerx[p], y, playerx[p] + playerw[p], y);
						shape.line(playerx[p], y - 2, playerx[p] + playerw[p], y - 2);
					}
					shape.end();
					sprite.begin();
					font.setColor(Color.valueOf("00c000"));
					font.draw(sprite, "BPM" + ((int) tl.getBPM()), playerx[0] + playerw[0] / 2, y + 20);
					sprite.end();
				}
				nbpm = tl.getBPM();
				// 小節線描画
				if (tl.getSectionLine()) {
					shape.begin(ShapeType.Line);
					for (int p = 0; p < playerx.length; p++) {
						shape.setColor(Color.GRAY);
						shape.line(playerx[p], y, playerx[p] + playerw[p], y);
					}
					shape.end();
				}
			} else if (pos == i - 1) {
				boolean b = true;
				for (int lane = 0; lane < laneregion.length; lane++) {
					Note note = tl.getNote(model.getUseKeys() == 9 && lane >= 5 ? lane + 5
							: (model.getUseKeys() > 9 && lane >= 8 ? lane + 1 : lane));
					if (note != null && note instanceof LongNote && ((LongNote) note).getEnd().getTime() >= time) {
						b = false;
						break;
					}
				}
				if (b) {
					pos = i;
				}
			}
			// ノート描画
			sprite.begin();
			for (int lane = 0; lane < laneregion.length; lane++) {
				Note note = tl.getNote(model.getUseKeys() == 9 && lane >= 5 ? lane + 5
						: (model.getUseKeys() > 9 && lane >= 8 ? lane + 1 : lane));
				if (note != null) {
					float x = laneregion[lane].x;
					float dx = laneregion[lane].width;
					float dy = 1;
					if (note instanceof LongNote) {
						if (((LongNote) note).getStart() == tl && ((LongNote) note).getEnd().getTime() >= time) {
							// if (((LongNote) note).getEnd() == null) {
							// Logger.getGlobal().warning(
							// "LN終端がなく、モデルが正常に表示されません。LN開始時間:"
							// + ((LongNote) note)
							// .getStart()
							// .getTime());
							// } else {
							double nbpm2 = tl.getBPM();
							dy = 0;
							for (int j = 0; timelines[i + j] != ((LongNote) note).getEnd(); j++) {
								if (timelines[i + j + 1].getTime() >= time) {
									if (nbpm2 > 0) {
										if (timelines[i + j].getTime() > time) {
											dy += (float) (timelines[i + j + 1].getTime() - timelines[i + j].getTime())
													* (hu - hl) / ((float) region / nbpm2 * bpm);
										} else {
											dy += (float) (timelines[i + j + 1].getTime() - time) * (hu - hl)
													/ ((float) region / nbpm2 * bpm);
										}
									}
									nbpm2 = timelines[i + j].getBPM();
								}
							}
						} else {
							dy = 0;
						}
					} else {
						if (timelines[i].getTime() < time) {
							dy = 0;
						}
					}
					if (dy > 0) {
						this.drawNote(x, y + dy, dx, dy, 18.0f, lane, note);
					}
				}
			}
			sprite.end();
		}

		sprite.begin();

		long[] bomb = judge.getBomb();
		// sprite.enableBlending();
		sprite.setBlendFunction(GL11.GL_ONE, GL11.GL_ONE);
		for (int lane = 0; lane < laneregion.length; lane++) {
			// ボム描画
			if (time >= bomb[lane]) {
				sprite.draw(
						skin.getBomb()[judge.getProcessingLongNotes()[lane] != null ? 2 : 0]
								.getKeyFrame((time - bomb[lane]) / 1000f),
						laneregion[lane].x + laneregion[lane].width / 2 - 110, hl - 155, 260, 270);
			}
		}
		sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		// sprite.disableBlending();

		final int judgenow = judge.getJudgeNow();
		final int judgenowt = judge.getJudgeTime();
		final int combo = judge.getCombo();
		// 判定文字描画

		if (judgenow > 0 && time < judgenowt + 500) {
			final float f = 1.5f;
			Sprite s = skin.getJudge()[(judgenow == 6 ? 5 : judgenow) - 1];
			int w = s.getRegionWidth();
			if (judgenow < 3) {
				w += 20;
				int count = 0;
				for (int i = combo;; count++) {
					w += 25;
					if (i < 10) {
						break;
					} else {
						i /= 10;
					}
				}
				for (int i = combo, j = 0;; j++) {
					sprite.draw(skin.getJudgenum()[judgenow - 1][i % 10],
							skin.getJudgeregion().x + skin.getJudgeregion().width / 2
									+ (-w / 2 + s.getRegionWidth() + 20 + (count - j) * 25) * f,
							hl + 100, 25 * f, 52 * f);
					if (i < 10) {
						break;
					} else {
						i /= 10;
					}
				}
			}
			sprite.draw(s, playerx[0] + playerw[0] / 2 - w * f / 2, hl + 100, s.getRegionWidth() * f,
					s.getRegionHeight() * f);
			// FAST, SLOW描画
			if (judgenow > 1) {
				font.setColor(judge.getJudgeTimingIsFast() ? Color.BLUE : Color.RED);
				font.draw(sprite, judge.getJudgeTimingIsFast() ? "FAST" : "SLOW", playerx[0] + playerw[0] / 2,
						hl + 180);

			}
		}

		// レーンカバー描画
		if (enableLanecover) {
			for (int p = 0; p < playerx.length; p++) {
				sprite.draw(skin.getLanecover(), playerx[p], hl + (hu - hl) * (1 - lanecover), playerw[p], (hu - hl));
				// 緑数字、白数字描画
				if (main.getBMSPlayerInputProcessor().startPressed()) {
					font.setColor(Color.WHITE);
					font.draw(sprite, String.format("%5d", Math.round(lanecover * 1000)),
							playerx[p] + playerw[p] * 0.25f, hl + (hu - hl) * (1 - lanecover));
					font.setColor(Color.GREEN);
					font.draw(sprite,
							String.format("%5d", Math.round(region * 0.6 * (1 - (enableLanecover ? lanecover : 0)))),
							playerx[p] + playerw[p] * 0.75f, hl + (hu - hl) * (1 - lanecover));
				}
			}
		}
		sprite.end();

	}

	private void drawNote(float x, float y, float width, float height, float scale, int lane, Note note) {
		if (note instanceof NormalNote) {
			sprite.draw(skin.getNote()[lane], x, y - scale / 2, width, scale);
		}
		if (note instanceof LongNote) {
			if (y - height < skin.getLaneregion()[lane].y) {
				height = y - skin.getLaneregion()[lane].y;
			}
			if (main.getJudgeManager().getProcessingLongNotes()[lane] == note) {
				sprite.draw(skin.getLongnote()[2][lane], x, y - height - 2, width, height + 4);
			} else {
				sprite.draw(skin.getLongnote()[3][lane], x, y - height - 2, width, height + 4);
			}
			sprite.draw(skin.getLongnote()[0][lane], x, y - scale / 2, width, scale);
			sprite.draw(skin.getLongnote()[1][lane], x, y - height - scale / 2, width, scale);
		}
		if (note instanceof MineNote) {
			sprite.draw(skin.getMinenote()[lane], x, y - scale / 2, width, scale);
		}
	}
}