package com.jog.play.entity;

/**
 * @create date 2015-8-17
 * @author Jog
 * @class description ������Ϣʵ����
 * */

public class MusicEntity {

	/** ����ID */
	private int id;

	/** ������ */
	private String title;

	/** �����ļ�·�� */
	private String filePath;

	/** ������ */
	private String artist;

	/** ��Ƭ�� */
	private String album;

	/** ����������Ϣ */
	private String otherInfo;

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getOtherInfo() {
		return otherInfo;
	}

	public void setOtherInfo(String otherInfo) {
		this.otherInfo = otherInfo;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}