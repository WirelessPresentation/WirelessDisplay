#include "airplay_player_mgr.h"

#include <assert.h>
#include <thread>
#include <algorithm>

#include "log.h"
#include "bj_airplay_def.h"

static const char* DEFAULT_DUMP_DIR =  "/sdcard";

AirplayPlayerMgr::AirplayPlayerMgr()
{
}

AirplayPlayerMgr::~AirplayPlayerMgr()
{
}

AirplayPlayerMgr& AirplayPlayerMgr::Instance()
{
	static AirplayPlayerMgr instance;
	return instance;
}

bool AirplayPlayerMgr::canCreatePlayer(uint32_t playerId)
{
	return true;
}

AirplayPlayer* AirplayPlayerMgr::findPlayerNoLock(uint32_t playerId)
{
	AirPLayerMapIt it = _playerMap.find(playerId);
	if (it != _playerMap.end())
	{
		return it->second;
	}
	return NULL;
}

void AirplayPlayerMgr::addPlayerNoLock(uint32_t playerId, AirplayPlayer* s)
{
	_playerMap[playerId] = s;
}

void AirplayPlayerMgr::rmvPlayerNoLock(uint32_t playerId)
{
	AirPLayerMapIt it = _playerMap.find(playerId);
	if (it != _playerMap.end())
	{
		it->second->stop();
		delete it->second;
		_playerMap.erase(it);
	}
}

uint32_t AirplayPlayerMgr::getSessionNum() const
{
    AirplayPlayerMgrLockGuard guard;
    return _playerMap.size();
}

AirplayPlayer::AirplayPlayer(uint32_t playerId)
	:_playerId(playerId)
{
}

AirplayPlayer::~AirplayPlayer()
{
}

int AirplayPlayer::getDuation()
{
	return 0;
}

int AirplayPlayer::getPts()
{
	return 0;
}

FileSink::FileSink(const char* path)
    :_file(NULL),_path(path)
{

}

FileSink::FileSink()
{
    close();
}

bool FileSink::open()
{
    _file = fopen(_path.c_str(),"wb");
    if(_file == NULL)
    {
        TRACE_ERR("fopen file %s failed",_path.c_str());
        return false;
    }
    return true;
}

void FileSink::close()
{
    if(_file)
    {
        fclose(_file);
        _file = NULL;
    }
}

void FileSink::write(char* data,int len)
{
    if(_file)
    {
        fwrite(data,1,len,_file);
    }
}

DemoMirrorPLayer::DemoMirrorPLayer(uint32_t playerId, const char* ip, const char* model, const char* device_name)
	:AirplayPlayer(playerId), _ip(ip), _model(model), _device_name(device_name)
{
    audio_sink = NULL;
    video_sink = NULL;
}

DemoMirrorPLayer::~DemoMirrorPLayer()
{
    if(audio_sink)
    {
        audio_sink->close();
        delete audio_sink;
        audio_sink = NULL;
    }

    if(video_sink)
    {
        video_sink->close();
        delete video_sink;
        video_sink = NULL;
    }
}


bool DemoMirrorPLayer::start()
{

	return true;
}

void DemoMirrorPLayer::stop()
{
    if(audio_sink)
    {
        audio_sink->close();
        delete audio_sink;
        audio_sink = NULL;
    }

    if(video_sink)
    {
        video_sink->close();
        delete video_sink;
        video_sink = NULL;
    }
}

void DemoMirrorPLayer::rendVideo(uint8_t* data, uint32_t size, long long ts)
{
    if(!video_sink)
    {
        char path[128] = {'\0'};
        snprintf(path,sizeof(path),"%s/mirror_video_%d.h264",DEFAULT_DUMP_DIR,_playerId);
        video_sink = new FileSink(path);
        if(!video_sink->open())
        {
            delete video_sink;
            video_sink = NULL;
        }
    }

    if(video_sink)
    {
        video_sink->write((char*)data, size);
    }
}

void DemoMirrorPLayer::rendAudio(uint8_t* audioData, uint32_t len, long long ts)
{
    if(!audio_sink)
    {
        char path[128] = {'\0'};
        snprintf(path,sizeof(path),"%s/mirror_audio_%d.pcm",DEFAULT_DUMP_DIR,_playerId);
        audio_sink = new FileSink(path);
        if(!audio_sink->open())
        {
            delete audio_sink;
            audio_sink = NULL;
        }
    }

    if(audio_sink)
    {
        audio_sink->write((char*)audioData, len);
    }
}

AirplayPlayerMgrLockGuard::AirplayPlayerMgrLockGuard(bool read /*= true*/)
{
	if (read)
	{
		AirplayPlayerMgr::Instance()._lock.read();
	}
	else
	{
		AirplayPlayerMgr::Instance()._lock.write();
	}
}

AirplayPlayerMgrLockGuard::~AirplayPlayerMgrLockGuard()
{
	AirplayPlayerMgr::Instance()._lock.unlock();
}

DemoAudioPLayer::DemoAudioPLayer(uint32_t playerId, const char* ip, const char* model, const char* device_name)
	:AirplayPlayer(playerId), _ip(ip), _model(model), _device_name(device_name)
{
    audio_sink = NULL;
}

DemoAudioPLayer::~DemoAudioPLayer()
{
    if(audio_sink)
    {
        audio_sink->close();
        delete audio_sink;
        audio_sink = NULL;
    }
}

bool DemoAudioPLayer::start()
{
	return true;
}

void DemoAudioPLayer::stop()
{

}

void DemoAudioPLayer::rendAudio(uint8_t* audioData, uint32_t len, long long ts)
{
    if(!audio_sink)
    {
        char path[128] = {'\0'};
        snprintf(path,sizeof(path),"%s/audio_%d.pcm",DEFAULT_DUMP_DIR,_playerId);
        audio_sink = new FileSink(path);
        if(!audio_sink->open())
        {
            delete audio_sink;
            audio_sink = NULL;
        }
    }

    if(audio_sink)
    {
        audio_sink->write((char*)audioData, len);
    }

}

DemoUrlPLayer::DemoUrlPLayer(uint32_t playerId, const char* ip, const char* url)
	:AirplayPlayer(playerId), _ip(ip)
{

}

DemoUrlPLayer::~DemoUrlPLayer()
{
}

bool DemoUrlPLayer::start()
{
	//TODO:Url 客户需要实现播放器对URL进行播放
	first = true;
	ptsSec = 1;
	play();
	return true;
}

void DemoUrlPLayer::stop()
{
	playIng = false;
}

void DemoUrlPLayer::play()
{
	playIng = true;
	paused = false;
}

void DemoUrlPLayer::pause()
{
	paused = true;
}

void DemoUrlPLayer::seek(int sec)
{
	ptsSec = sec;
}

int DemoUrlPLayer::getDuation()
{
	//URL的播放总时长，单位为毫秒，此处假设是2小时:7200*1000
	return 7200 * 1000;
}

int DemoUrlPLayer::getPts()
{
	//当前播放位置，单位是毫秒
	if (playIng && !paused)
	{
		return (ptsSec++) * 1000;
	}

	return (ptsSec) * 1000;
}

int DemoUrlPLayer::getStatus()  //需要根据实际情况返回状态，最开始应该返回loading
{
    if(first)
    {
        first = false;
        return BJ_PLAYER_STATUS_LOADING;
    }

    if(playIng)
    {
        if(paused)
            return BJ_PLAYER_STATUS_PAUSED;

        return BJ_PLAYER_STATUS_PLAYING;
    }

    return BJ_PLAYER_STATUS_ENDED;
}

