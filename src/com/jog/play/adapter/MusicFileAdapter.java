package com.jog.play.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jog.play5.R;
import com.jog.play.application.JogPlayerApplication;
import com.jog.play.entity.MusicEntity;

/**
 * @create date 2015-8-17
 * @author Jog
 * @class description �����ļ��б�
 */

public class MusicFileAdapter extends BaseAdapter {

	/** ������ */
	private Context mContext;

	/** �����ļ��б� */
	private ArrayList<MusicEntity> mList;

	public MusicFileAdapter(Context context, ArrayList<MusicEntity> list) {
		this.mContext = context;
		this.mList = list;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder mHolder = null;

		if (convertView == null) {
			mHolder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.music_list_item, null);
			mHolder.mMusicName = (TextView) convertView
					.findViewById(R.id.music);
			mHolder.mArtistInfo = (TextView) convertView
					.findViewById(R.id.artistInfo);
			mHolder.mSign = (View) convertView.findViewById(R.id.sign);
			convertView.setTag(mHolder);
		} else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		// �ж��Ƿ�Ϊ���һ�β��ŵ�����
		if (mList.get(position).getId() == JogPlayerApplication.appMusicID) {
			mHolder.mSign.setBackgroundColor(Color.parseColor("#7EC0EE"));
		} else {
			mHolder.mSign.setBackgroundColor(Color.parseColor("#FFFFFF"));
		}
		mHolder.mMusicName.setText(mList.get(position).getTitle());
		mHolder.mArtistInfo.setText(mList.get(position).getArtist() + " - "
				+ mList.get(position).getAlbum());
		return convertView;
	}

	private class ViewHolder {
		private TextView mMusicName, mArtistInfo;
		private View mSign;
	}

}