#include "bj_airplay_callback_imp.h"

#include <assert.h>
#include <memory>

#include "log.h"
#include "bj_airplay.h"
#include "bj_airplay_def.h"

#include "airplay_player_mgr.h"

int32_t BJAirplayCallbackImp::OnStartMirrorPlayer(uint32_t playerId, const char* ip, const char* device_model, const char* device_name)
{
	TRACE_DBG("OnStartMirrorPlayer");
	TRACE_DBG("OnStartMirrorPlayer id:%u ip:%s,device_model:%s device_name:%s", playerId,ip,device_model,device_name);
	std::unique_ptr<DemoMirrorPLayer> player(new DemoMirrorPLayer(playerId, ip, device_model, device_name));
	bool res = player->start();
	if (res)
	{
		AirplayPlayerMgrLockGuard guard(false);
		AirplayPlayerMgr::Instance().addPlayerNoLock(playerId, player.release());
        return 0; //success
	}

	return 1;
}

void BJAirplayCallbackImp::NotifyMirrorAudioCodecInfo(uint32_t playerId, int32_t bitsdep, int32_t channels, int32_t sampleRate)
{
	TRACE_DBG("NotifyMirrorAudioCodecInfo id:%u bitsdep:%d,channels:%d sampleRate:%d", playerId, bitsdep, channels,sampleRate);
	AirplayPlayerMgrLockGuard guard(true);
}

uint32_t BJAirplayCallbackImp::OnMirrorAudioData(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue)
{
	//TRACE_DBG("OnMirrorAudioData id:%u size:%u", playerId, size);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
		return size;
	}
	DemoMirrorPLayer* mirrorPlayer = dynamic_cast<DemoMirrorPLayer*>(player);
	if (mirrorPlayer == nullptr)
	{
		assert(false);
		return size;
	}
	mirrorPlayer->rendAudio((uint8_t *)p_src, size, ptsValue);
	return size;
}

uint32_t BJAirplayCallbackImp::OnMirrorVideoData(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue)
{
	//TRACE_DBG("OnMirrorVideoData id:%u size:%u", playerId, size);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
		return size;
	}
	DemoMirrorPLayer* mirrorPlayer = dynamic_cast<DemoMirrorPLayer*>(player);
	if (mirrorPlayer == nullptr)
	{
		assert(false);
		return size;
	}
	mirrorPlayer->rendVideo((uint8_t *)p_src, size, ptsValue);

	return size;
}

void BJAirplayCallbackImp::OnRotateMirrorVideo(uint32_t playerId, int angle)
{
	//cur rotation is 0,do nothing
}

void BJAirplayCallbackImp::OnStopMirrorPlayer(uint32_t playerId)
{
	TRACE_DBG("OnStopMirrorPlayer id:%u ", playerId);
	AirplayPlayerMgrLockGuard guard(false);
	AirplayPlayerMgr::Instance().rmvPlayerNoLock(playerId);
}

int32_t BJAirplayCallbackImp::OnStartAudioPlayer(uint32_t playerId, const char* ip, const char* device_model, const char* device_name)
{
	TRACE_DBG("OnStartAudioPlayer id:%u ip:%s,device_model:%s device_name:%s", playerId, ip, device_model, device_name);
	std::unique_ptr<DemoAudioPLayer> player(new DemoAudioPLayer(playerId, ip, device_model, device_name));
	bool res = player->start();
	if (res)
	{
		AirplayPlayerMgrLockGuard guard(false);
		AirplayPlayerMgr::Instance().addPlayerNoLock(playerId, player.release());
        return 0;
	}

	return 1;
}

uint32_t BJAirplayCallbackImp::OnAudioData(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue)
{
	//TRACE_DBG("OnMirrorAudioData id:%u size:%u", playerId, size);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
		return size;
	}
	DemoAudioPLayer* mirrorPlayer = dynamic_cast<DemoAudioPLayer*>(player);
	if (mirrorPlayer == nullptr)
	{
		assert(false);
		return size;
	}
	mirrorPlayer->rendAudio((uint8_t *)p_src, size, ptsValue);
	return size;
}

void BJAirplayCallbackImp::RefreshCoverArtFromBuffer(uint32_t playerId, const uint8_t *p_src, int size)
{
	char fname[128] = { '0' };
	snprintf(fname, 128, "/sdcard/music_%u.jpeg", playerId);
	FILE* file = fopen(fname, "wb");
	if (file){
		int nwrite = fwrite(p_src, size, 1, file);
		fclose(file);
	}
    TRACE_INFO("RefreshCoverArtFromBuffer dump to %s",fname);
}

void BJAirplayCallbackImp::SetVolume(uint32_t playerId, int volumePercent)
{
	TRACE_DBG("SetVolume id:%u volumePercent:%f", playerId, volumePercent);
}

void BJAirplayCallbackImp::OnStopAudioPlayer(uint32_t playerId)
{
	TRACE_DBG("OnStopAudioPlayer id:%u", playerId);
	AirplayPlayerMgrLockGuard guard(false);
	AirplayPlayerMgr::Instance().rmvPlayerNoLock(playerId);
}

void BJAirplayCallbackImp::NotifyAudioCodecInfo(uint32_t playerId, int32_t bitsdep, int32_t channels, int32_t sampleRate)
{
	TRACE_DBG("NotifyAudioCodecInfo id:%u bitsdep:%d,channels:%d sampleRate:%d", playerId, bitsdep, channels, sampleRate);
}

void BJAirplayCallbackImp::RefreshTrackInfo(uint32_t playerId, const char* album, const char* title, const char* artist)
{
	TRACE_DBG("RefreshTrackInfo id:%u album:%s,title:%s artist:%s", playerId, album, title, artist);
}

int32_t BJAirplayCallbackImp::OnStartVideoPlayback(uint32_t playerId, const char *ip, const char *url)
{
	TRACE_DBG("OnStartVideoPlayback id:%u ip:%s,url:%s", playerId, ip, url);
	std::unique_ptr<DemoUrlPLayer> player(new DemoUrlPLayer(playerId, ip, url));
	bool res = player->start();
	if (res)
	{
		{
		AirplayPlayerMgrLockGuard guard(false);
		AirplayPlayerMgr::Instance().addPlayerNoLock(playerId, player.release());
		}

        return 0;
	}

	return 1;
}

void BJAirplayCallbackImp::OnStopVideoPlayback(uint32_t playerId)
{
	TRACE_DBG("OnStopVideoPlayback id:%u", playerId);
	AirplayPlayerMgrLockGuard guard(false);
	AirplayPlayerMgr::Instance().rmvPlayerNoLock(playerId);
}

void BJAirplayCallbackImp::OnPauseVideoPlayback(uint32_t playerId)
{
	TRACE_DBG("OnPauseVideoPlayback id:%u ", playerId);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
		return;
	}
	DemoUrlPLayer* urlPlayer = dynamic_cast<DemoUrlPLayer*>(player);
	if (urlPlayer == nullptr)
	{
		assert(false);
		return;
	}
	urlPlayer->pause();
	return;
}

void BJAirplayCallbackImp::OnResumeVideoPlayback(uint32_t playerId)
{
	TRACE_DBG("OnResumeVideoPlayback id:%u ", playerId);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
		return;
	}
	DemoUrlPLayer* urlPlayer = dynamic_cast<DemoUrlPLayer*>(player);
	if (urlPlayer == nullptr)
	{
		assert(false);
		return;
	}
	urlPlayer->play();
}

void BJAirplayCallbackImp::OnSeekVideoBySec(uint32_t playerId, int64_t position)
{
	TRACE_DBG("OnSeekVideoBySec id:%u %I64d", playerId, position);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
		return;
	}
	DemoUrlPLayer* urlPlayer = dynamic_cast<DemoUrlPLayer*>(player);
	if (urlPlayer == nullptr)
	{
		assert(false);
		return;
	}
	urlPlayer->seek(position);
}

int32_t BJAirplayCallbackImp::OnGetVideoPositionMSec(uint32_t playerId)
{
	TRACE_DBG("OnGetVideoPositionMSec id:%u ", playerId);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
	return 0;
	}
	DemoUrlPLayer* urlPlayer = dynamic_cast<DemoUrlPLayer*>(player);
	if (urlPlayer == nullptr)
	{
		assert(false);
		return 0;
	}
	return urlPlayer->getPts();
}

int32_t BJAirplayCallbackImp::OnGetVideoDurationMSec(uint32_t playerId)
{
	TRACE_DBG("OnGetVideoDurationMSec id:%u ", playerId);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
	return 0;
	}
	DemoUrlPLayer* urlPlayer = dynamic_cast<DemoUrlPLayer*>(player);
	if (urlPlayer == nullptr)
	{
		assert(false);
		return 0;
	}
	return urlPlayer->getDuation();
}

int32_t BJAirplayCallbackImp::OnGetVideoPlayerStatus(uint32_t playerId)
{
	TRACE_DBG("OnGetVideoPlayerStatus id:%u ", playerId);
	AirplayPlayerMgrLockGuard guard(true);
	AirplayPlayer* player = AirplayPlayerMgr::Instance().findPlayerNoLock(playerId);
	if (player == nullptr)
	{
	    return BJ_PLAYER_STATUS_LOADING;
	}
	DemoUrlPLayer* urlPlayer = dynamic_cast<DemoUrlPLayer*>(player);
	if (urlPlayer == nullptr)
	{
		assert(false);
		return BJ_PLAYER_STATUS_LOADING;
	}
    int32_t ret  = urlPlayer->getStatus();

    TRACE_DBG("OnGetVideoPlayerStatus id:%u ret:%d", playerId,ret);
    return ret;
}

void BJAirplayCallbackImp::OnShowPinCode(const char* ip,const char* pincode)
{
    TRACE_DBG("OnShowPinCode ip:%s pincode:%s", ip,pincode);
}

/**PIN码验证成功
* @param ip
* @return void
* @note Android sdk暂未实现
*/
void BJAirplayCallbackImp::OnVerifyPinSuccess(const char* ip)
{
    TRACE_DBG("OnVerifyPinSuccess ip:%s ", ip);
}

void BJAirplayCallbackImp::OnProbePlayerAbility(const char* ip,  BJAirPlayAbility* const ability)
{
    int size = AirplayPlayerMgr::Instance().getSessionNum();
    if(size >= 1)
    {
        ability->maxFPS = 20;
        ability->resolution_type = BJAir_Resolution_720P;
    }
    else
    {
        ability->maxFPS = 30;
        ability->resolution_type = BJAir_Resolution_1080P;
    }
}

int32_t BJAirplayCallbackImp::OnStartPhotoPlayer(uint32_t playerId, const char* ip)
{
    TRACE_DBG("OnStartPhotoPlayer playerId:%u ip:%s ", playerId,ip);
    return 0;
}

void BJAirplayCallbackImp::OnPlayPhoto(uint32_t playerId, const uint8_t* picData, int size)
{
    static int id = 0;

    char fname[128] = { '0' };
	snprintf(fname, 128, "/sdcard/pic_%u_%d.jpeg", playerId,id++);
	FILE* file = fopen(fname, "wb");
	if (file){
		int nwrite = fwrite(picData, size, 1, file);
		fclose(file);
	}

    TRACE_DBG("OnStartPlayPhoto playerId:%u pic dump to %s", playerId,fname);
}


void BJAirplayCallbackImp::OnStopPhotoPlayer(uint32_t playerId)
{
    TRACE_DBG("OnStopPlayPhoto playerId:%u", playerId);
}

