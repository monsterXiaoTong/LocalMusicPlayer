package com.jog.play.adapter;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jog.play5.R;

/**
 * @create date 2015-8-17
 * @author Jog
 * @class description 文件夹列表 适配器
 */

public class FoldListAdapter extends BaseAdapter {

	/** 上下文 */
	private Context mContext;

	/** 主列表 */
	private ArrayList<Map<String, String>> mList;

	public FoldListAdapter(Context context, ArrayList<Map<String, String>> list) {
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
					R.layout.folder_list_item, null);
			mHolder.mFileCount = (TextView) convertView
					.findViewById(R.id.count);
			mHolder.mFolderName = (TextView) convertView
					.findViewById(R.id.name);
			mHolder.mFolderPath = (TextView) convertView
					.findViewById(R.id.path);

			convertView.setTag(mHolder);
		} else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		mHolder.mFolderName.setText(mList.get(position).get("FoldName"));
		mHolder.mFolderPath.setText(mList.get(position).get("PartFoldPath"));
		mHolder.mFileCount.setText(mList.get(position).get("FileCount"));
		return convertView;
	}

	private class ViewHolder {
		private TextView mFolderName, mFileCount, mFolderPath;
	}
}
