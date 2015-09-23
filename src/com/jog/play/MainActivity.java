package com.jog.play;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.jog.play.adapter.FoldListAdapter;
import com.jog.play.adapter.MusicFileAdapter;
import com.jog.play.application.JogPlayerApplication;
import com.jog.play.entity.MusicEntity;
import com.jog.play.service.MusicPlayerService;
import com.jog.play5.R;

/**
 * @create date 2015-8-18
 * @author Jog
 * @class description ���ֲ�����������
 * */

public class MainActivity extends Activity implements OnClickListener {

	/** ��ǰ���������ļ����б����� */
	private int mMusicListID;

	/** ��ǰ�������ֵ���ʱ�� */
	private int currentMusicDuration;

	/** ��ǰ�������ֵ�����·�� */
	private String mMusicFullPath;

	/** ��ǰ�������� / ���һ�β��ŵ����� */
	private TextView lastMusic;

	/** ��ǰ�����������ڵ������ļ��б� */
	private ArrayList<MusicEntity> mCurrentMusicList;

	/** ����������ļ��� ���� */
	private String selectFolderName;

	/** ����������ļ�������·�� */
	private String selectFolderPath;

	/** ���ڲ��ŵ��������ڵ��ļ���·�� */
	private String playMusicFolderPath;

	/** ������ */
	private PopupWindow popup;

	/** �ļ�����Ϣ�б� */
	private ArrayList<Map<String, String>> mFolderList;

	/** �����ļ��б� */
	private ArrayList<MusicEntity> mMusicList;

	/** ���ذ�ť */
	private LinearLayout mBackLayout;

	/** ����ͼƬ */
	private ImageView mBackPic;

	/** �������� */
	private TextView mTitleView;

	/** ���ఴť */
	private LinearLayout mMoreLayout;

	/* �ļ��б� */
	private ListView mFoldListView, mMusicFileListView;

	/* ���ƽ��� */

	/** ���Ž��� */
	private TextView mPassTime, mTotalTime;

	/** ������ */
	private SeekBar mVolumeBar, mProgressBar;

	/** ���ſ��� */
	private ImageView mPreviousBtn, mPlayBtn, mNextBtn;

	/** �ļ��������� */
	private FoldListAdapter mFoldAdapter;

	/** �����ļ������� */
	private MusicFileAdapter mMusicFileAdapter;

	/** �л����� */
	private boolean isRoot = true;

	/** ˫���˳����� */
	private boolean isFirstBack = false;

	/** �������϶����� */
	private boolean volumeChanging = false, progressChanging = false,
			physicsButtonControling = false;

	/** �в��ż�¼ */
	private boolean hasMusicHistory = false;

	/** ���ش洢 */
	private SharedPreferences mPreferences;

	/** ���ش洢�༭�� */
	private SharedPreferences.Editor mEditor;

	/** ���ֲ��ŷ��� */
	private MusicPlayerService mService;

	/** ��Ƶ������� */
	private AudioManager mAudioManager;

	/** �������ڹ㲥������ */
	private MediaVolumeReceiver mVolumeReceiver;

	/** �������ڹ㲥������ */
	private MediaProgressReceiver mProgressReceiver;

	/** ʮ��������ʽ������ */
	private DecimalFormat formatter = new DecimalFormat("#00");

	/** �л����� */
	private Animation backBtnIn, bananaBtnIn, fragmentRightIn, fragmentLeftOut,
			fragmentRightOut, fragmentLeftIn;

	/** ����ģʽ�л���ť */
	private TextView loopModeBtn;

	/** ����ģʽ���� */
	private boolean singleMusicLoop = false;

	/** ��Ϣ������ */
	private Handler mHandler = new Handler() {
		public void handleMessage(Message ms) {
			switch (ms.what) {
			case 0x00:
				// ������ʾ���ı�
				String newMusic = (String) ms.obj;
				lastMusic.setText("��ǰ���Ÿ���: " + newMusic);
				mPlayBtn.setImageResource(R.drawable.play);
				// ����ָ������
				mService.setupMediaPlayer(mMusicFullPath);

				break;
			case 0x01:
				// �����ļ����б�
				outOfFolder();

				break;
			case 0x02:
				// ���ļ����б� ���� ĳһ�����ļ���
				inToFolder();
				break;

			case 0x03:
				int progress = (int) ms.obj;
				mPassTime.setText(convertMusicDuration(progress));

				mProgressBar.setProgress(mService.getCurrentPosition()
						* mProgressBar.getMax() / currentMusicDuration);
				break;

			case 0x04:
				// ����ֹͣ����λ
				mPassTime.setText("00:00");
				mProgressBar.setProgress(0);
				mPlayBtn.setImageResource(R.drawable.stop);

				if (!singleMusicLoop) {
					// ����ѭ��ģʽ������֮�����¿�ʼ
					// ����˳�򲥷�ģʽ������֮�󲥷���һ���������²����б��еĵ�һ��
					if (mMusicListID < mCurrentMusicList.size() - 1) {
						mMusicListID++;
					} else {
						mMusicListID = 0;
					}
				}

				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						mHandler.sendEmptyMessage(0x06);
					}

				}, 2000);

				break;

			case 0x05:
				// resume 1��֮����ʾ��ʷ������Ϣ
				String welcomeStr = (String) ms.obj;
				lastMusic.setText(welcomeStr);
				break;

			case 0x06:
				// ����ָ�������б��е��ļ�
				playSelectedMusic(mCurrentMusicList.get(mMusicListID));
				checkRangaLegal();
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		// ��ʼ�����
		findAllViews();

		// �����ļ����б�
		updateFolderListView();

		// �����ֲ��ŷ���
		bindPlayMusicService();

	}

	@Override
	protected void onResume() {
		super.onResume();

		// ��̨��ʷ���ݼ��
		checkOutHistory();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// ȡ��ע�������
		unregisterReceiver(mVolumeReceiver);
		unregisterReceiver(mProgressReceiver);
	}

	/**
	 * ���ֲ��Ž���ɨ���߳�
	 * */
	private class MusicScanTask extends TimerTask {

		@Override
		public void run() {
			mHandler.sendEmptyMessage(0x03);
		}
	};

	private class MyThread extends Thread {
		public void run() {
			mHandler.sendEmptyMessage(0x03);
		}
	}

	/**
	 * ����ָ������
	 * */
	private void bindPlayMusicService() {

		// ��ʽָ�����󶨵Ķ���
		Intent intent = new Intent();
		intent.setAction("jog.player.service");
		intent.setPackage(getPackageName());

		bindService(intent, new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName,
					IBinder binder) {
				// ����bindService������������ʱ�����������Ҫ��activity������
				// ��ͨ��onBind��������IBinder�����ص�ǰ���ط���
				mService = ((MusicPlayerService.LocalBinder) binder)
						.getService();
				// ���������ʾ�û�,���ߵ��÷����ĳЩ����

			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {

				mService = null;
				// ���������ʾ�û�
			}
		}, Context.BIND_AUTO_CREATE);
	}

	/**
	 * �����²�����ʷ
	 * */
	private void checkOutHistory() {
		mPreferences = getSharedPreferences("JogPlayer", Context.MODE_PRIVATE);
		mEditor = mPreferences.edit();

		int historyID = mPreferences.getInt("LastMusicID", -1);
		JogPlayerApplication.appMusicID = historyID;

		lastMusic.setText(getResources().getString(R.string.welcome_word));

		// ������ʾ����
		if (historyID != -1) {
			String musicName = mPreferences.getString("LastMusicName", "");
			String musicArtist = mPreferences.getString("LastMusicArtist", "");
			String musicAlbum = mPreferences.getString("LastMusicAlbum", "");
			final String welcomeStr = "��ǰ������Ŀ: " + musicName + " - "
					+ musicArtist + " - " + musicAlbum;
			mMusicFullPath = mPreferences.getString("LastMusicFullPath", "");
			currentMusicDuration = mPreferences.getInt("LastMusicDuration", 1);
			playMusicFolderPath = mPreferences.getString("PlayMusicFolderPath",
					"");

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mTotalTime
							.setText(convertMusicDuration(currentMusicDuration));
					lastMusic.setText(welcomeStr);
				}
			}, 2000);

			hasMusicHistory = true;

			if (mMusicList.size() == 0) {
				mPreviousBtn.setEnabled(false);
				mNextBtn.setEnabled(false);
			}

		} else {
			mPreviousBtn.setEnabled(false);
			mPlayBtn.setEnabled(false);
			mNextBtn.setEnabled(false);
		}

	}

	/**
	 * ��ĳһ�����ļ��� ���ص� �ļ����б�
	 * */
	private void outOfFolder() {
		mTitleView.setText("JogPlayer");
		mFoldListView.setVisibility(View.VISIBLE);
		mMusicFileListView.setVisibility(View.GONE);
		mBackPic.setImageResource(R.drawable.bananas);
		isRoot = true;

		// ����
		mBackPic.startAnimation(bananaBtnIn);
		mFoldListView.startAnimation(fragmentLeftIn);
		mMusicFileListView.startAnimation(fragmentRightOut);

		mNextBtn.setEnabled(false);
		mPreviousBtn.setEnabled(false);
	}

	/**
	 * ���ļ����б� ���� ĳһ�����ļ���
	 * */
	private void inToFolder() {
		isRoot = false;
		mBackPic.setImageResource(R.drawable.back);
		mTitleView.setText(selectFolderName);
		mFoldListView.setVisibility(View.GONE);
		mMusicFileListView.setVisibility(View.VISIBLE);

		// ����
		mBackPic.startAnimation(backBtnIn);
		mFoldListView.startAnimation(fragmentLeftOut);
		mMusicFileListView.startAnimation(fragmentRightIn);

		// ��ȡ���ļ����е����������ļ�
		updateMusicListView();

		// �����ļ��к���°�ť״̬
		updateBtnStateAfterResume();
	}

	/**
	 * �����ļ��к���°�ť״̬
	 * */
	private void updateBtnStateAfterResume() {
		for (int i = 0; i < mMusicList.size(); i++) {
			MusicEntity entity = mMusicList.get(i);
			if (entity.getId() == JogPlayerApplication.appMusicID) {
				mMusicListID = i;
			}
		}
		checkRangaLegal();
	}

	/**
	 * ��ȡָ���ļ����е����������ļ�
	 * */
	private void updateMusicListView() {
		mMusicList.clear();

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				null);
		// MediaStore.Images.Media.DATE_ADDED + " DESC"
		try {
			// ��ʼ����
			while (cursor != null && cursor.moveToNext()) {
				// #1 ��������·��
				String fullPath = cursor.getString(cursor
						.getColumnIndex(Media.DATA));

				// ƥ���Ƿ�Ϊָ���ļ����µ������ļ�
				String tempArray[] = fullPath.split("/");
				int tempLenght = tempArray.length;
				String mFolderFullPath = fullPath.substring(0,
						fullPath.length() - tempArray[tempLenght - 1].length()
								- tempArray[tempLenght - 2].length() - 1);

				if (mFolderFullPath.equals(selectFolderPath)) {
					MusicEntity entity = new MusicEntity();
					// ����ID
					int mID = Integer.valueOf(cursor.getString(cursor
							.getColumnIndex(Media._ID)));
					entity.setId(mID);
					// ����ȫ·��
					entity.setFilePath(fullPath);
					// ������
					String musicTitle = cursor.getString(cursor
							.getColumnIndex(Media.TITLE));
					entity.setTitle(musicTitle);
					// ����
					String musicArtist = cursor.getString(cursor
							.getColumnIndex(Media.ARTIST));
					entity.setArtist(musicArtist);
					// ר��
					String musicAlbum = cursor.getString(cursor
							.getColumnIndex(Media.ALBUM));
					entity.setAlbum(musicAlbum);
					// ʱ��
					String musicDuration = cursor.getString(cursor
							.getColumnIndex(Media.DURATION));
					entity.setOtherInfo(musicDuration);
					mMusicList.add(entity);
				}

			}
		} finally {
			cursor.close();
		}

		mMusicFileAdapter.notifyDataSetChanged();
	}

	/**
	 * ��ʼ�������е����
	 * */
	private void findAllViews() {

		mFolderList = new ArrayList<Map<String, String>>();
		mMusicList = new ArrayList<MusicEntity>();

		loopModeBtn = (TextView) findViewById(R.id.loopMode);
		loopModeBtn.setOnClickListener(this);

		/* ������ */
		mBackLayout = (LinearLayout) findViewById(R.id.back_layout);
		mBackPic = (ImageView) findViewById(R.id.back_btn);
		mMoreLayout = (LinearLayout) findViewById(R.id.more_layout);
		mTitleView = (TextView) findViewById(R.id.title);

		/* �б� */
		mFoldListView = (ListView) findViewById(R.id.mFoldList);
		mFoldListView.setVisibility(View.VISIBLE);
		mMusicFileListView = (ListView) findViewById(R.id.mMusicList);
		mMusicFileListView.setVisibility(View.GONE);

		/* ���ƽ��� */
		mPassTime = (TextView) findViewById(R.id.passTime);
		mTotalTime = (TextView) findViewById(R.id.totalTime);
		mVolumeBar = (SeekBar) findViewById(R.id.vollume);
		mProgressBar = (SeekBar) findViewById(R.id.play_progress);
		mPreviousBtn = (ImageView) findViewById(R.id.previous);
		mPlayBtn = (ImageView) findViewById(R.id.play);
		mNextBtn = (ImageView) findViewById(R.id.next);

		mFoldAdapter = new FoldListAdapter(this, mFolderList);
		mFoldListView.setAdapter(mFoldAdapter);
		mFoldListView.setOnItemClickListener(new FolderItemClickListener());

		mPlayBtn.setOnClickListener(this);
		mPreviousBtn.setOnClickListener(this);
		mNextBtn.setOnClickListener(this);
		mBackLayout.setOnClickListener(this);
		mMoreLayout.setOnClickListener(this);

		mMusicFileAdapter = new MusicFileAdapter(this, mMusicList);
		mMusicFileListView.setAdapter(mMusicFileAdapter);
		mMusicFileListView.setOnItemClickListener(new MusicItemClickListener());

		lastMusic = (TextView) findViewById(R.id.currentMusic);

		mProgressBar
				.setOnSeekBarChangeListener(new ProgressBarChangeListener());
		mVolumeBar.setOnSeekBarChangeListener(new VolumeBarChangeListener());
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// android5.0���� �������ֵΪ60��֮ǰ��Ϊ15
		int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		volume = volume * mVolumeBar.getMax() / 60;
		mVolumeBar.setProgress(volume);

		// ע��ϵͳ����������
		mVolumeReceiver = new MediaVolumeReceiver();
		IntentFilter filter1 = new IntentFilter();
		filter1.addAction("android.media.VOLUME_CHANGED_ACTION");
		registerReceiver(mVolumeReceiver, filter1);

		// ע�����ֲ��Ž��ȼ�����
		mProgressReceiver = new MediaProgressReceiver();
		IntentFilter filter2 = new IntentFilter();
		filter2.addAction("jog.player.service.PROGRESS");
		filter2.addAction("jog.player.service.COMPLETITION");
		registerReceiver(mProgressReceiver, filter2);

		mProgressBar.setEnabled(false);

		backBtnIn = AnimationUtils.loadAnimation(this, R.anim.back_button_in);
		bananaBtnIn = AnimationUtils.loadAnimation(this,
				R.anim.banana_button_in);
		fragmentRightIn = AnimationUtils.loadAnimation(this,
				R.anim.fragment_right_in);
		fragmentLeftOut = AnimationUtils.loadAnimation(this,
				R.anim.fragment_left_out);
		fragmentRightOut = AnimationUtils.loadAnimation(this,
				R.anim.fragment_right_out);
		fragmentLeftIn = AnimationUtils.loadAnimation(this,
				R.anim.fragment_left_in);
	}

	/**
	 * �Զ������ֲ��Ž��ȼ�����
	 * */
	private class ProgressBarChangeListener implements OnSeekBarChangeListener {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			int musicProgress = progress * currentMusicDuration
					/ mProgressBar.getMax();
			// mService.setPlayIndex(musicProgress);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			progressChanging = true;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			progressChanging = false;
		}
	}

	/**
	 * �Զ�������SeekBar������
	 * */
	private class VolumeBarChangeListener implements OnSeekBarChangeListener {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {

			if (!physicsButtonControling) {
				int index = (int) (progress / ((float) mVolumeBar.getMax()) * 60);
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index,
						0);
			}

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			volumeChanging = true;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			volumeChanging = false;
		}

	}

	/**
	 * �ļ����б������¼�������
	 * */
	private class FolderItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			HashMap<String, String> folderEntity = (HashMap<String, String>) mFolderList
					.get(position);
			selectFolderPath = folderEntity.get("FullFoldPath");
			selectFolderName = folderEntity.get("FoldName");

			mHandler.sendEmptyMessage(0x02);
		}
	}

	/**
	 * �����б������¼�������
	 * */
	private class MusicItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mMusicListID = position;
			MusicEntity mEntity = mMusicList.get(position);

			// ��¼��ǰ�����������ڵ������ļ��б�
			if (mCurrentMusicList == null) {
				mCurrentMusicList = new ArrayList<MusicEntity>();

			}

			if (mCurrentMusicList.size() > 0) {
				mCurrentMusicList.clear();
			}

			mCurrentMusicList.addAll(mMusicList);

			// �����ͬ������Ŀ��������
			if (!(mEntity.getFilePath().equals(mMusicFullPath) && mService
					.isPlaying())) {

				mPlayBtn.setEnabled(true);

				playSelectedMusic(mEntity);

				checkRangaLegal();

			}

		}
	}

	/**
	 * ����ָ������
	 * */
	private void playSelectedMusic(MusicEntity entity) {

		JogPlayerApplication.appMusicID = entity.getId();

		mMusicFullPath = entity.getFilePath();

		// ������ʾ���ı�������ָ������
		String hintStr = entity.getTitle() + " - " + entity.getArtist() + " - "
				+ entity.getAlbum();
		Message msg = mHandler.obtainMessage();
		msg.obj = hintStr;
		msg.what = 0x00;
		mHandler.sendMessage(msg);

		mMusicFileAdapter.notifyDataSetChanged();

		// ƽ������
		// mMusicFileListView.smoothScrollToPositionFromTop(mMusicListID, 0);

		// ת������ʱ��
		currentMusicDuration = Integer.valueOf(entity.getOtherInfo());
		mTotalTime.setText(convertMusicDuration(currentMusicDuration));

		// �־û�������Ϣ
		saveEntityLocal(entity);
	}

	/**
	 * �־û�������Ϣ������
	 * */
	private void saveEntityLocal(MusicEntity mEntity) {

		// ��¼���ڲ����������ڵ��ļ���·��
		String fullPath = mEntity.getFilePath();
		String[] fullPathArray = fullPath.split("/");
		playMusicFolderPath = fullPath.substring(0, fullPath.length()
				- fullPathArray[fullPathArray.length - 1].length()
				- fullPathArray[fullPathArray.length - 2].length() - 1);

		// ���沥���ļ���Ϣ������
		mEditor.putString("PlayMusicFolderPath", playMusicFolderPath);
		mEditor.putString("LastMusicName", mEntity.getTitle());
		mEditor.putString("LastMusicArtist", mEntity.getArtist());
		mEditor.putString("LastMusicAlbum", mEntity.getAlbum());
		mEditor.putInt("LastMusicID", mEntity.getId());
		mEditor.putString("LastMusicFullPath", mEntity.getFilePath());
		mEditor.putInt("LastMusicDuration",
				Integer.valueOf(mEntity.getOtherInfo()));
		mEditor.commit();
	}

	/**
	 * ת������ʱ��
	 * */
	private String convertMusicDuration(int millisec) {
		int minite = millisec / 1000 / 60;
		int sec = (int) (millisec / 1000.0 % 60);
		return formatter.format(minite) + ":" + formatter.format(sec);
	}

	/**
	 * ���������ļ��� �����б�
	 * */
	private void updateFolderListView() {

		mFolderList.clear();

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				null);
		// MediaStore.Images.Media.DATE_ADDED + " DESC"
		try {
			// ��ʼ����
			while (cursor != null && cursor.moveToNext()) {
				// #1 ��������·��
				String fullPath = cursor.getString(cursor
						.getColumnIndex(Media.DATA));

				// #2 ��⡢��ȡ�����ļ�������
				String[] tempArray = fullPath.split("/");
				int tempLenght = tempArray.length;
				String folderName = tempArray[tempLenght - 3];

				// #3 ��ȡ�ļ��в���·��
				String mFolderPartPath = "";
				if (tempLenght >= 4) {
					mFolderPartPath = tempArray[tempLenght - 4] + "/"
							+ tempArray[tempLenght - 3];
				} else {
					mFolderPartPath = tempArray[tempLenght - 3];
				}

				// #4 ��ȡ�ļ�������·��

				String mFolderFullPath = fullPath.substring(0,
						fullPath.length() - tempArray[tempLenght - 1].length()
								- tempArray[tempLenght - 2].length() - 1);

				if (cursor.isFirst()) {
					Map<String, String> newPath1 = new HashMap<String, String>();
					// ����һ���ļ�����Ϣ��ӵ�������
					newPath1.put("FoldName", folderName);
					newPath1.put("FileCount", String.valueOf(1));
					newPath1.put("PartFoldPath", mFolderPartPath);
					newPath1.put("FullFoldPath", mFolderFullPath);
					mFolderList.add(newPath1);
				} else {
					for (int i = 0; i < mFolderList.size(); i++) {
						// ���ڼ����в��޴������Ӹ���
						if (!(mFolderList.get(i).containsValue(mFolderPartPath))
								&& i == mFolderList.size() - 1) {
							Map<String, String> newPath = new HashMap<String, String>();
							// Key (a new list-member)
							newPath.put("FoldName", folderName);
							newPath.put("FileCount", String.valueOf(1));
							newPath.put("PartFoldPath", mFolderPartPath);
							newPath.put("FullFoldPath", mFolderFullPath);
							mFolderList.add(newPath);
							break;
						} else if (mFolderList.get(i).containsValue(
								mFolderPartPath)) {
							// "FileCount" add one
							int count = Integer.valueOf(mFolderList.get(i).get(
									"FileCount"));
							mFolderList.get(i).put("FileCount",
									String.valueOf(++count));
							break;
						}
					}
				}
			}
		} finally {
			cursor.close();
		}

		mFoldAdapter.notifyDataSetChanged();

	}

	/**
	 * ����¼�������
	 * */
	@Override
	public void onClick(View v) {

		int viewID = v.getId();

		switch (viewID) {
		case R.id.play:
			// ����ǰΪ����״̬
			if (mService.isPlaying()) {
				mPlayBtn.setImageResource(R.drawable.pause);
				hasMusicHistory = false;
				mService.pause();
			} else {

				if (hasMusicHistory) {
					mService.setupMediaPlayer(mMusicFullPath);
					hasMusicHistory = false;
				}
				// ����ǰΪ��ͣ״̬
				mPlayBtn.setImageResource(R.drawable.play);
				mService.play();
			}
			break;

		case R.id.next:
			// ��һ��
			if (mMusicListID < mMusicList.size()) {
				mMusicListID++;
				playSelectedMusic(mMusicList.get(mMusicListID));
				checkRangaLegal();
			}
			break;

		case R.id.previous:
			// ��һ��
			if (mMusicListID > 0) {
				mMusicListID--;
				playSelectedMusic(mMusicList.get(mMusicListID));
				checkRangaLegal();
			}
			break;

		case R.id.back_layout:
			// �����ļ����б�
			if (!isRoot) {
				mBackPic.setImageDrawable(getResources().getDrawable(
						R.drawable.bananas));
				isRoot = true;

				// �����л�
				mHandler.sendEmptyMessage(0x01);
			}
			break;
		case R.id.more_layout:
			// ���ఴť
			showPopupMenu();
			break;

		case R.id.loopMode:
			// ���ֲ���ģʽ���ư�ť
			if (singleMusicLoop) {
				// �л�������ѭ��
				singleMusicLoop = false;
				loopModeBtn.setText("˳�򲥷�");
			} else {
				// �л���˳�򲥷�
				singleMusicLoop = true;
				loopModeBtn.setText("����ѭ��");
			}
			break;
		}

	}

	/**
	 * ͨ���жϸ������кţ����ð�ť�Ŀɵ����
	 * */
	private void checkRangaLegal() {

		// ֻ�н��뵱ǰ�������ֵ��ļ��вſ���һ��/��һ���л�
		if (selectFolderPath.equals(playMusicFolderPath)) {
			mNextBtn.setEnabled(true);
			mPreviousBtn.setEnabled(true);

			if (mMusicListID == mMusicList.size() - 1) {
				mNextBtn.setEnabled(false);
			} else if (mMusicListID == 0) {
				mPreviousBtn.setEnabled(false);
			}
		} else {
			mNextBtn.setEnabled(false);
			mPreviousBtn.setEnabled(false);
		}

	}

	/**
	 * ���ݵ�¼�û�Ȩ�� װ��Menu
	 * 
	 * @return void
	 * */
	private void showPopupMenu() {
		View root = this.getLayoutInflater().inflate(R.layout.popup, null);

		popup = new PopupWindow(root, 200, LayoutParams.WRAP_CONTENT);
		ListView list = (ListView) root.findViewById(R.id.listViewMenu);

		popup.setOutsideTouchable(true);
		ColorDrawable colorDrawable = new ColorDrawable(Color.WHITE);
		colorDrawable.setAlpha(255);
		popup.setBackgroundDrawable(colorDrawable);
		// popup.setBackgroundDrawable(new BitmapDrawable());
		// popup.setBackgroundDrawable(new BitmapDrawable(null,""));

		ArrayList<String> strList = new ArrayList<String>();
		strList.add("a");
		strList.add("b");

		// �������Ͻǵ����˵�
		ArrayAdapter adapter = new ArrayAdapter<String>(MainActivity.this,
				R.layout.list_item_dropdown, strList);
		list.setAdapter(adapter);
		popup.showAsDropDown(mMoreLayout, 0, 8);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (!isRoot) {
				outOfFolder();
				return true;
			} else {
				if (!isFirstBack) {
					// ��һ�δ��������ؼ���
					isFirstBack = true;
					Toast.makeText(MainActivity.this, "˫���������˳�",
							Toast.LENGTH_SHORT).show();
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							isFirstBack = false;
						}
					}, 900);
					return true;
				} else {
					// ��900ms��ʱ����ǰ�ٴδ��������˳�
					System.exit(0);
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * ���������仯ʱ�Ľ�����ʾ
	 * 
	 * @author
	 */
	private class MediaVolumeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!volumeChanging) {
				physicsButtonControling = true;
				// ������������仯�������seekbar��λ��
				// android5.0���� �������ֵΪ60��֮ǰ��Ϊ15
				int volume = mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				volume = volume * mVolumeBar.getMax() / 60;
				mVolumeBar.setProgress(volume);
				physicsButtonControling = false;
			}
		}
	}

	/**
	 * �������ֲ��ŷ������Ľ�����Ϣ
	 * */
	private class MediaProgressReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if ("jog.player.service.COMPLETITION".equals(intent.getAction())) {
				// ��λ
				mHandler.sendEmptyMessage(0x04);

			} else if ("jog.player.service.PROGRESS".equals(intent.getAction())
					&& !progressChanging) {
				// ���½�����ʾ
				int progress = intent.getIntExtra("progress", -1);
				Message msg = mHandler.obtainMessage();
				msg.what = 0x03;
				msg.obj = progress;
				mHandler.sendMessage(msg);
			}

		}
	}
}
