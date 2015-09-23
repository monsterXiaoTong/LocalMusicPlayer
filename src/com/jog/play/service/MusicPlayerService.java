package com.jog.play.service;

import java.util.Timer;
import java.util.TimerTask;

import com.jog.play5.R;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

/**
 * @create date 2015-8-19
 * @author Jog
 * @class description 音乐播放服务类
 */

public class MusicPlayerService extends Service {

	/** 音频播放控件 */
	private MediaPlayer mMediaPlayer;

	/** 服务端与访问者之间通信的接口 */
	private IBinder binder = new MusicPlayerService.LocalBinder();

	/**
	 * 返回Binder对象给访问者
	 * */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/**
	 * 只在第一次创建时回调，多次启动只回调一次
	 * */
	@Override
	public void onCreate() {
		super.onCreate();
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnCompletionListener(new MusicCompletionListener());
		mMediaPlayer.setOnPreparedListener(new MusicPreparedListener());

	}

	/**
	 * 自定义Binder子类
	 * */
	public class LocalBinder extends Binder {

		// 返回本地服务
		public MusicPlayerService getService() {
			return MusicPlayerService.this;
		}
	}

	/**
	 * 配置音频播放器
	 * */
	public void setupMediaPlayer(String musicPath) {
		try {
			mMediaPlayer.reset();
			mMediaPlayer.setDataSource(musicPath);
			mMediaPlayer.prepare();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 开始播放
	 * */
	public void play() {
		mMediaPlayer.start();
	}

	/**
	 * 暂停播放
	 * */
	public void pause() {
		mMediaPlayer.pause();
	}

	/**
	 * 暂停播放
	 * */
	public boolean isPlaying() {
		return mMediaPlayer.isPlaying();
	}

	/**
	 * 当前播放进度值
	 * */
	public int getCurrentPosition() {
		return mMediaPlayer.getCurrentPosition();
	}

	/**
	 * 指定播放进度
	 * */
	public void setPlayIndex(int progress) {
		mMediaPlayer.seekTo(progress);
	}

	/**
	 * 自定义播放结束监听器
	 * */
	private class MusicCompletionListener implements OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			mMediaPlayer.reset();
			Intent i = new Intent();
			i.setAction("jog.player.service.COMPLETITION");
			sendBroadcast(i);
		}
	}

	/**
	 * 音乐播放准备就绪监听器
	 * */
	private class MusicPreparedListener implements OnPreparedListener {
		@Override
		public void onPrepared(MediaPlayer mp) {

			// 准备就绪后开始播放
			if (!mMediaPlayer.isPlaying()) {
				mMediaPlayer.start();
			}

			// 定时获取播放进度
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					// 给主界面发送
					if (mMediaPlayer.isPlaying()) {
						Intent i = new Intent();
						i.putExtra("progress",
								mMediaPlayer.getCurrentPosition());
						i.setAction("jog.player.service.PROGRESS");
						sendBroadcast(i);
					}

				}
			}, 0, 1000);

		}
	}

	/**
	 * 销毁时回调
	 * */
	@Override
	public void onDestroy() {

		super.onDestroy();
	}

}