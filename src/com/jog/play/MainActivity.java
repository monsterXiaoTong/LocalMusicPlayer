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
 * @class description 音乐播放器主界面
 * */

public class MainActivity extends Activity implements OnClickListener {

	/** 当前音乐所在文件夹列表的序号 */
	private int mMusicListID;

	/** 当前播放音乐的总时长 */
	private int currentMusicDuration;

	/** 当前播放音乐的完整路径 */
	private String mMusicFullPath;

	/** 当前播放音乐 / 最后一次播放的音乐 */
	private TextView lastMusic;

	/** 当前播放音乐所在的音乐文件列表 */
	private ArrayList<MusicEntity> mCurrentMusicList;

	/** 进入的音乐文件夹 名称 */
	private String selectFolderName;

	/** 进入的音乐文件夹完整路径 */
	private String selectFolderPath;

	/** 正在播放的音乐所在的文件夹路径 */
	private String playMusicFolderPath;

	/** 弹出窗 */
	private PopupWindow popup;

	/** 文件夹信息列表 */
	private ArrayList<Map<String, String>> mFolderList;

	/** 音乐文件列表 */
	private ArrayList<MusicEntity> mMusicList;

	/** 返回按钮 */
	private LinearLayout mBackLayout;

	/** 返回图片 */
	private ImageView mBackPic;

	/** 标题文字 */
	private TextView mTitleView;

	/** 更多按钮 */
	private LinearLayout mMoreLayout;

	/* 文件列表 */
	private ListView mFoldListView, mMusicFileListView;

	/* 控制界面 */

	/** 播放进度 */
	private TextView mPassTime, mTotalTime;

	/** 滑动条 */
	private SeekBar mVolumeBar, mProgressBar;

	/** 播放控制 */
	private ImageView mPreviousBtn, mPlayBtn, mNextBtn;

	/** 文件夹适配器 */
	private FoldListAdapter mFoldAdapter;

	/** 音乐文件适配器 */
	private MusicFileAdapter mMusicFileAdapter;

	/** 切换标量 */
	private boolean isRoot = true;

	/** 双击退出标量 */
	private boolean isFirstBack = false;

	/** 滑动条拖动标量 */
	private boolean volumeChanging = false, progressChanging = false,
			physicsButtonControling = false;

	/** 有播放记录 */
	private boolean hasMusicHistory = false;

	/** 本地存储 */
	private SharedPreferences mPreferences;

	/** 本地存储编辑器 */
	private SharedPreferences.Editor mEditor;

	/** 音乐播放服务 */
	private MusicPlayerService mService;

	/** 音频管理对象 */
	private AudioManager mAudioManager;

	/** 音量调节广播接收器 */
	private MediaVolumeReceiver mVolumeReceiver;

	/** 音量调节广播接收器 */
	private MediaProgressReceiver mProgressReceiver;

	/** 十进制数格式化对象 */
	private DecimalFormat formatter = new DecimalFormat("#00");

	/** 切换动画 */
	private Animation backBtnIn, bananaBtnIn, fragmentRightIn, fragmentLeftOut,
			fragmentRightOut, fragmentLeftIn;

	/** 播放模式切换按钮 */
	private TextView loopModeBtn;

	/** 播放模式标量 */
	private boolean singleMusicLoop = false;

	/** 消息处理器 */
	private Handler mHandler = new Handler() {
		public void handleMessage(Message ms) {
			switch (ms.what) {
			case 0x00:
				// 更新提示窗文本
				String newMusic = (String) ms.obj;
				lastMusic.setText("当前播放歌曲: " + newMusic);
				mPlayBtn.setImageResource(R.drawable.play);
				// 播放指定音乐
				mService.setupMediaPlayer(mMusicFullPath);

				break;
			case 0x01:
				// 返回文件夹列表
				outOfFolder();

				break;
			case 0x02:
				// 从文件夹列表 进入 某一音乐文件夹
				inToFolder();
				break;

			case 0x03:
				int progress = (int) ms.obj;
				mPassTime.setText(convertMusicDuration(progress));

				mProgressBar.setProgress(mService.getCurrentPosition()
						* mProgressBar.getMax() / currentMusicDuration);
				break;

			case 0x04:
				// 播放停止，复位
				mPassTime.setText("00:00");
				mProgressBar.setProgress(0);
				mPlayBtn.setImageResource(R.drawable.stop);

				if (!singleMusicLoop) {
					// 单曲循环模式，两秒之后重新开始
					// 否则，顺序播放模式，两秒之后播放下一曲，或重新播放列表中的第一首
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
				// resume 1秒之后，提示历史播放信息
				String welcomeStr = (String) ms.obj;
				lastMusic.setText(welcomeStr);
				break;

			case 0x06:
				// 播放指定音乐列表中的文件
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

		// 初始化组件
		findAllViews();

		// 更新文件夹列表
		updateFolderListView();

		// 绑定音乐播放服务
		bindPlayMusicService();

	}

	@Override
	protected void onResume() {
		super.onResume();

		// 后台历史数据检测
		checkOutHistory();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// 取消注册接收器
		unregisterReceiver(mVolumeReceiver);
		unregisterReceiver(mProgressReceiver);
	}

	/**
	 * 音乐播放进度扫描线程
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
	 * 播放指定音乐
	 * */
	private void bindPlayMusicService() {

		// 显式指定待绑定的对象
		Intent intent = new Intent();
		intent.setAction("jog.player.service");
		intent.setPackage(getPackageName());

		bindService(intent, new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName,
					IBinder binder) {
				// 调用bindService方法启动服务时候，如果服务需要与activity交互，
				// 则通过onBind方法返回IBinder并返回当前本地服务
				mService = ((MusicPlayerService.LocalBinder) binder)
						.getService();
				// 这里可以提示用户,或者调用服务的某些方法

			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {

				mService = null;
				// 这里可以提示用户
			}
		}, Context.BIND_AUTO_CREATE);
	}

	/**
	 * 检查更新播放历史
	 * */
	private void checkOutHistory() {
		mPreferences = getSharedPreferences("JogPlayer", Context.MODE_PRIVATE);
		mEditor = mPreferences.edit();

		int historyID = mPreferences.getInt("LastMusicID", -1);
		JogPlayerApplication.appMusicID = historyID;

		lastMusic.setText(getResources().getString(R.string.welcome_word));

		// 更新提示标题
		if (historyID != -1) {
			String musicName = mPreferences.getString("LastMusicName", "");
			String musicArtist = mPreferences.getString("LastMusicArtist", "");
			String musicAlbum = mPreferences.getString("LastMusicAlbum", "");
			final String welcomeStr = "当前播放曲目: " + musicName + " - "
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
	 * 从某一音乐文件夹 返回到 文件夹列表
	 * */
	private void outOfFolder() {
		mTitleView.setText("JogPlayer");
		mFoldListView.setVisibility(View.VISIBLE);
		mMusicFileListView.setVisibility(View.GONE);
		mBackPic.setImageResource(R.drawable.bananas);
		isRoot = true;

		// 动画
		mBackPic.startAnimation(bananaBtnIn);
		mFoldListView.startAnimation(fragmentLeftIn);
		mMusicFileListView.startAnimation(fragmentRightOut);

		mNextBtn.setEnabled(false);
		mPreviousBtn.setEnabled(false);
	}

	/**
	 * 从文件夹列表 进入 某一音乐文件夹
	 * */
	private void inToFolder() {
		isRoot = false;
		mBackPic.setImageResource(R.drawable.back);
		mTitleView.setText(selectFolderName);
		mFoldListView.setVisibility(View.GONE);
		mMusicFileListView.setVisibility(View.VISIBLE);

		// 动画
		mBackPic.startAnimation(backBtnIn);
		mFoldListView.startAnimation(fragmentLeftOut);
		mMusicFileListView.startAnimation(fragmentRightIn);

		// 获取该文件夹中的所有音乐文件
		updateMusicListView();

		// 进入文件夹后更新按钮状态
		updateBtnStateAfterResume();
	}

	/**
	 * 进入文件夹后更新按钮状态
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
	 * 获取指定文件夹中的所有音乐文件
	 * */
	private void updateMusicListView() {
		mMusicList.clear();

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				null);
		// MediaStore.Images.Media.DATE_ADDED + " DESC"
		try {
			// 开始迭代
			while (cursor != null && cursor.moveToNext()) {
				// #1 歌曲完整路径
				String fullPath = cursor.getString(cursor
						.getColumnIndex(Media.DATA));

				// 匹配是否为指定文件夹下的音乐文件
				String tempArray[] = fullPath.split("/");
				int tempLenght = tempArray.length;
				String mFolderFullPath = fullPath.substring(0,
						fullPath.length() - tempArray[tempLenght - 1].length()
								- tempArray[tempLenght - 2].length() - 1);

				if (mFolderFullPath.equals(selectFolderPath)) {
					MusicEntity entity = new MusicEntity();
					// 歌曲ID
					int mID = Integer.valueOf(cursor.getString(cursor
							.getColumnIndex(Media._ID)));
					entity.setId(mID);
					// 歌曲全路径
					entity.setFilePath(fullPath);
					// 歌曲名
					String musicTitle = cursor.getString(cursor
							.getColumnIndex(Media.TITLE));
					entity.setTitle(musicTitle);
					// 歌手
					String musicArtist = cursor.getString(cursor
							.getColumnIndex(Media.ARTIST));
					entity.setArtist(musicArtist);
					// 专辑
					String musicAlbum = cursor.getString(cursor
							.getColumnIndex(Media.ALBUM));
					entity.setAlbum(musicAlbum);
					// 时长
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
	 * 初始化界面中的组件
	 * */
	private void findAllViews() {

		mFolderList = new ArrayList<Map<String, String>>();
		mMusicList = new ArrayList<MusicEntity>();

		loopModeBtn = (TextView) findViewById(R.id.loopMode);
		loopModeBtn.setOnClickListener(this);

		/* 标题栏 */
		mBackLayout = (LinearLayout) findViewById(R.id.back_layout);
		mBackPic = (ImageView) findViewById(R.id.back_btn);
		mMoreLayout = (LinearLayout) findViewById(R.id.more_layout);
		mTitleView = (TextView) findViewById(R.id.title);

		/* 列表 */
		mFoldListView = (ListView) findViewById(R.id.mFoldList);
		mFoldListView.setVisibility(View.VISIBLE);
		mMusicFileListView = (ListView) findViewById(R.id.mMusicList);
		mMusicFileListView.setVisibility(View.GONE);

		/* 控制界面 */
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
		// android5.0以上 最大音量值为60；之前的为15
		int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		volume = volume * mVolumeBar.getMax() / 60;
		mVolumeBar.setProgress(volume);

		// 注册系统音量监听器
		mVolumeReceiver = new MediaVolumeReceiver();
		IntentFilter filter1 = new IntentFilter();
		filter1.addAction("android.media.VOLUME_CHANGED_ACTION");
		registerReceiver(mVolumeReceiver, filter1);

		// 注册音乐播放进度监听器
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
	 * 自定义音乐播放进度监听器
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
	 * 自定义音量SeekBar监听器
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
	 * 文件夹列表项点击事件监听器
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
	 * 音乐列表项点击事件监听器
	 * */
	private class MusicItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mMusicListID = position;
			MusicEntity mEntity = mMusicList.get(position);

			// 记录当前播放音乐所在的音乐文件列表
			if (mCurrentMusicList == null) {
				mCurrentMusicList = new ArrayList<MusicEntity>();

			}

			if (mCurrentMusicList.size() > 0) {
				mCurrentMusicList.clear();
			}

			mCurrentMusicList.addAll(mMusicList);

			// 点击相同歌曲曲目不做处理
			if (!(mEntity.getFilePath().equals(mMusicFullPath) && mService
					.isPlaying())) {

				mPlayBtn.setEnabled(true);

				playSelectedMusic(mEntity);

				checkRangaLegal();

			}

		}
	}

	/**
	 * 播放指定音乐
	 * */
	private void playSelectedMusic(MusicEntity entity) {

		JogPlayerApplication.appMusicID = entity.getId();

		mMusicFullPath = entity.getFilePath();

		// 更新提示窗文本，播放指定音乐
		String hintStr = entity.getTitle() + " - " + entity.getArtist() + " - "
				+ entity.getAlbum();
		Message msg = mHandler.obtainMessage();
		msg.obj = hintStr;
		msg.what = 0x00;
		mHandler.sendMessage(msg);

		mMusicFileAdapter.notifyDataSetChanged();

		// 平滑滚动
		// mMusicFileListView.smoothScrollToPositionFromTop(mMusicListID, 0);

		// 转换音乐时长
		currentMusicDuration = Integer.valueOf(entity.getOtherInfo());
		mTotalTime.setText(convertMusicDuration(currentMusicDuration));

		// 持久化歌曲信息
		saveEntityLocal(entity);
	}

	/**
	 * 持久化歌曲信息到本地
	 * */
	private void saveEntityLocal(MusicEntity mEntity) {

		// 记录正在播放音乐所在的文件夹路径
		String fullPath = mEntity.getFilePath();
		String[] fullPathArray = fullPath.split("/");
		playMusicFolderPath = fullPath.substring(0, fullPath.length()
				- fullPathArray[fullPathArray.length - 1].length()
				- fullPathArray[fullPathArray.length - 2].length() - 1);

		// 保存播放文件信息到本地
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
	 * 转换音乐时长
	 * */
	private String convertMusicDuration(int millisec) {
		int minite = millisec / 1000 / 60;
		int sec = (int) (millisec / 1000.0 % 60);
		return formatter.format(minite) + ":" + formatter.format(sec);
	}

	/**
	 * 更新音乐文件夹 数据列表
	 * */
	private void updateFolderListView() {

		mFolderList.clear();

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				null);
		// MediaStore.Images.Media.DATE_ADDED + " DESC"
		try {
			// 开始迭代
			while (cursor != null && cursor.moveToNext()) {
				// #1 歌曲完整路径
				String fullPath = cursor.getString(cursor
						.getColumnIndex(Media.DATA));

				// #2 拆解、获取音乐文件夹名称
				String[] tempArray = fullPath.split("/");
				int tempLenght = tempArray.length;
				String folderName = tempArray[tempLenght - 3];

				// #3 获取文件夹部分路径
				String mFolderPartPath = "";
				if (tempLenght >= 4) {
					mFolderPartPath = tempArray[tempLenght - 4] + "/"
							+ tempArray[tempLenght - 3];
				} else {
					mFolderPartPath = tempArray[tempLenght - 3];
				}

				// #4 获取文件夹完整路径

				String mFolderFullPath = fullPath.substring(0,
						fullPath.length() - tempArray[tempLenght - 1].length()
								- tempArray[tempLenght - 2].length() - 1);

				if (cursor.isFirst()) {
					Map<String, String> newPath1 = new HashMap<String, String>();
					// 将第一个文件夹信息添加到集合中
					newPath1.put("FoldName", folderName);
					newPath1.put("FileCount", String.valueOf(1));
					newPath1.put("PartFoldPath", mFolderPartPath);
					newPath1.put("FullFoldPath", mFolderFullPath);
					mFolderList.add(newPath1);
				} else {
					for (int i = 0; i < mFolderList.size(); i++) {
						// 若在集合中查无此项，则添加该项
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
	 * 点击事件监听器
	 * */
	@Override
	public void onClick(View v) {

		int viewID = v.getId();

		switch (viewID) {
		case R.id.play:
			// 若当前为播放状态
			if (mService.isPlaying()) {
				mPlayBtn.setImageResource(R.drawable.pause);
				hasMusicHistory = false;
				mService.pause();
			} else {

				if (hasMusicHistory) {
					mService.setupMediaPlayer(mMusicFullPath);
					hasMusicHistory = false;
				}
				// 若当前为暂停状态
				mPlayBtn.setImageResource(R.drawable.play);
				mService.play();
			}
			break;

		case R.id.next:
			// 下一首
			if (mMusicListID < mMusicList.size()) {
				mMusicListID++;
				playSelectedMusic(mMusicList.get(mMusicListID));
				checkRangaLegal();
			}
			break;

		case R.id.previous:
			// 上一首
			if (mMusicListID > 0) {
				mMusicListID--;
				playSelectedMusic(mMusicList.get(mMusicListID));
				checkRangaLegal();
			}
			break;

		case R.id.back_layout:
			// 返回文件夹列表
			if (!isRoot) {
				mBackPic.setImageDrawable(getResources().getDrawable(
						R.drawable.bananas));
				isRoot = true;

				// 返回切换
				mHandler.sendEmptyMessage(0x01);
			}
			break;
		case R.id.more_layout:
			// 更多按钮
			showPopupMenu();
			break;

		case R.id.loopMode:
			// 音乐播放模式控制按钮
			if (singleMusicLoop) {
				// 切换到单曲循环
				singleMusicLoop = false;
				loopModeBtn.setText("顺序播放");
			} else {
				// 切换到顺序播放
				singleMusicLoop = true;
				loopModeBtn.setText("单曲循环");
			}
			break;
		}

	}

	/**
	 * 通过判断歌曲序列号，设置按钮的可点击性
	 * */
	private void checkRangaLegal() {

		// 只有进入当前播放音乐的文件夹才可上一曲/下一曲切换
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
	 * 根据登录用户权限 装载Menu
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

		// 适配右上角弹出菜单
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
					// 第一次触发“返回键”
					isFirstBack = true;
					Toast.makeText(MainActivity.this, "双击返回则退出",
							Toast.LENGTH_SHORT).show();
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							isFirstBack = false;
						}
					}, 900);
					return true;
				} else {
					// 在900ms定时任务前再次触发，则退出
					System.exit(0);
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 处理音量变化时的界面显示
	 * 
	 * @author
	 */
	private class MediaVolumeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!volumeChanging) {
				physicsButtonControling = true;
				// 如果音量发生变化，则更改seekbar的位置
				// android5.0以上 最大音量值为60；之前的为15
				int volume = mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				volume = volume * mVolumeBar.getMax() / 60;
				mVolumeBar.setProgress(volume);
				physicsButtonControling = false;
			}
		}
	}

	/**
	 * 接收音乐播放服务发来的进度信息
	 * */
	private class MediaProgressReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if ("jog.player.service.COMPLETITION".equals(intent.getAction())) {
				// 复位
				mHandler.sendEmptyMessage(0x04);

			} else if ("jog.player.service.PROGRESS".equals(intent.getAction())
					&& !progressChanging) {
				// 更新进度显示
				int progress = intent.getIntExtra("progress", -1);
				Message msg = mHandler.obtainMessage();
				msg.what = 0x03;
				msg.obj = progress;
				mHandler.sendMessage(msg);
			}

		}
	}
}
