#pragma once

#include <stdint.h>
#include <string>
#include <map>
#include "common_lock.h"

enum PlayerType
{
	MirrorPlayer,
	AudioPLayer,
	UrlPLayer,
	PlayerBut
};

class FileSink
{
public:
    FileSink(const char* path);
    FileSink();
    bool open();
    void close();

    void write(char* data,int len);
private:
    FILE* _file;
    std::string _path;
};

class AirplayPlayer
{
public:
	AirplayPlayer(uint32_t playerId);
	virtual ~AirplayPlayer();
	virtual int getPlayerType() = 0;

	uint32_t getPlayerId() const
	{
		return _playerId;
	}
	virtual bool start() = 0;
	virtual void stop() = 0;

	virtual int getDuation();
	virtual int getPts();
protected:
	std::string _ip;
	uint32_t _playerId;
};

class DemoMirrorPLayer
	:public AirplayPlayer
{

public:
	DemoMirrorPLayer(uint32_t playerId, const char* ip, const char* model, const char* device_name);
	~DemoMirrorPLayer();

	virtual int getPlayerType(){
		return MirrorPlayer;
	}
	virtual bool start();
	virtual void stop();

	void rendVideo(uint8_t* videoData, uint32_t len, long long ts);
	void rendAudio(uint8_t* audioData, uint32_t len, long long ts);

private:
	std::string _ip;
	std::string _model;
	std::string _device_name;

    FileSink* audio_sink;
    FileSink* video_sink;
};

class DemoAudioPLayer
	:public AirplayPlayer
{

public:
	DemoAudioPLayer(uint32_t playerId, const char* ip, const char* model, const char* device_name);
	~DemoAudioPLayer();

	virtual int getPlayerType(){
		return AudioPLayer;
	}
	virtual bool start();
	virtual void stop();

	void rendAudio(uint8_t* audioData, uint32_t len, long long ts);

private:
	std::string _ip;
	std::string _model;
	std::string _device_name;
    FileSink* audio_sink;
};


class DemoUrlPLayer
	:public AirplayPlayer
{

public:
	DemoUrlPLayer(uint32_t playerId, const char* ip, const char* url);
	~DemoUrlPLayer();

	virtual int getPlayerType(){
		return UrlPLayer;
	}
	virtual bool start();
	virtual void stop();

	void play();  //播放
	void pause();	//暂停
	void seek(int sec);	//进度条拖动到第几秒

	void rendAudio(uint8_t* audioData, uint32_t len, long long ts);

	virtual int getDuation();
	virtual int getPts();
    int getStatus();

private:
	std::string _ip;
	int ptsSec;

	bool playIng;
	bool paused;
    int status;

    bool first;
};

typedef std::map<uint32_t, AirplayPlayer*> AirPLayerMap;
typedef std::map<uint32_t, AirplayPlayer*>::iterator AirPLayerMapIt;

class AirplayPlayerMgrLockGuard
{
public:
	AirplayPlayerMgrLockGuard(bool read = true);
	~AirplayPlayerMgrLockGuard();
};

class AirplayPlayerMgr
{
public:
	~AirplayPlayerMgr();

	static AirplayPlayerMgr& Instance();

	bool canCreatePlayer(uint32_t playerId);
	AirplayPlayer* findPlayerNoLock(uint32_t playerId);
	void addPlayerNoLock(uint32_t playerId, AirplayPlayer* s);
	void rmvPlayerNoLock(uint32_t playerId);
    uint32_t getSessionNum() const;
private:
	friend class AirplayPlayerMgrLockGuard;
	AirplayPlayerMgr();
	AirPLayerMap _playerMap;

	RWLock _lock;
};

