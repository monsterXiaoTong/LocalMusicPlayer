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
 * @class description ���ֲ��ŷ�����
 */

public class MusicPlayerService extends Service {

	/** ��Ƶ���ſؼ� */
	private MediaPlayer mMediaPlayer;

	/** ������������֮��ͨ�ŵĽӿ� */
	private IBinder binder = new MusicPlayerService.LocalBinder();

	/**
	 * ����Binder�����������
	 * */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/**
	 * ֻ�ڵ�һ�δ���ʱ�ص����������ֻ�ص�һ��
	 * */
	@Override
	public void onCreate() {
		super.onCreate();
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnCompletionListener(new MusicCompletionListener());
		mMediaPlayer.setOnPreparedListener(new MusicPreparedListener());

	}

	/**
	 * �Զ���Binder����
	 * */
	public class LocalBinder extends Binder {

		// ���ر��ط���
		public MusicPlayerService getService() {
			return MusicPlayerService.this;
		}
	}

	/**
	 * ������Ƶ������
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
	 * ��ʼ����
	 * */
	public void play() {
		mMediaPlayer.start();
	}

	/**
	 * ��ͣ����
	 * */
	public void pause() {
		mMediaPlayer.pause();
	}

	/**
	 * ��ͣ����
	 * */
	public boolean isPlaying() {
		return mMediaPlayer.isPlaying();
	}

	/**
	 * ��ǰ���Ž���ֵ
	 * */
	public int getCurrentPosition() {
		return mMediaPlayer.getCurrentPosition();
	}

	/**
	 * ָ�����Ž���
	 * */
	public void setPlayIndex(int progress) {
		mMediaPlayer.seekTo(progress);
	}

	/**
	 * �Զ��岥�Ž���������
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
	 * ���ֲ���׼������������
	 * */
	private class MusicPreparedListener implements OnPreparedListener {
		@Override
		public void onPrepared(MediaPlayer mp) {

			// ׼��������ʼ����
			if (!mMediaPlayer.isPlaying()) {
				mMediaPlayer.start();
			}

			// ��ʱ��ȡ���Ž���
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					// �������淢��
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
	 * ����ʱ�ص�
	 * */
	@Override
	public void onDestroy() {

		super.onDestroy();
	}

}