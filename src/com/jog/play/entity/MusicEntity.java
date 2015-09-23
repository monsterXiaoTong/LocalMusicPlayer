package com.jog.play.entity;

/**
 * @create date 2015-8-17
 * @author Jog
 * @class description 歌曲信息实体类
 * */

public class MusicEntity {

	/** 歌曲ID */
	private int id;

	/** 歌曲名 */
	private String title;

	/** 歌曲文件路径 */
	private String filePath;

	/** 艺术家 */
	private String artist;

	/** 唱片名 */
	private String album;

	/** 歌曲其他信息 */
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