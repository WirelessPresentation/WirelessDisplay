#pragma once
#include "bj_airplay.h"

class BJAirplayCallbackImp
{
public:

	static int32_t OnStartMirrorPlayer(uint32_t playerId, const char* ip, const char* device_model, const char* device_name);

	/**
	* \brief           Write mirror audio PCM frame to player.
	* \param bitsdep   bitsdep of audio, 16 is default.
	* \param channels  channels of audio, 2 is default.
	* \param sampleRate sampleRate of audio, 44100 is default. .
	* \return          void
	*/
	static void NotifyMirrorAudioCodecInfo(uint32_t playerId, int32_t bitsdep, int32_t channels, int32_t sampleRate);

	/** 通知应用镜像音频数据
	*
	*     镜像投屏过程中，SDK收到音频数据后会进行解码，并调用该接口通知应用层已经解码的PCM音频数据。
	*     @param playerId 播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param p_src   指向视频数据快的指针
	*     @param size    视频数据快的长度
	*     @param ptsValue 视频数据对应的时间戳
	*     @return Copied size. (unit: byte)
	*     @note   应用收到音频数据后因进行解码并播放，同时需要根据时间戳与音频做同步控制。
	*/
	static uint32_t OnMirrorAudioData(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue);

	/** 通知应用镜像视频数据
	*
	*     镜像投屏过程中，SDK收到视频数据后会调用该接口通知应用层收到到H264视频数据。
	*     @param playerId 播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param p_src 指向视频数据快的指针
	*     @param size    视频数据快的长度
	*     @param ptsValue 视频数据对应的时间戳
	*     @return Copied size. (unit: byte)
	*     @see    NotifyMirrorAudioCodecInfo()  音频的采样率信息通过NotifyMirrorAudioCodecInfo通知应用
	*     @note   应用收到视频数据后因进行解码并播放，同时需要根据时间戳与音频做同步控制。
	*/
	static uint32_t OnMirrorVideoData(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue);

	/** 镜像播放视频旋转角度通知
	*
	*     当IPAD/IPHONE等终端
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param angle 视频旋转角度，可能取值范围为0,90,180,270。所有角度为顺时针方向角度。
	*     @return    无
	*     @note   当应用收到该事件后，播放器需要根据该角度来处理旋转，才能正确显示视频。
	*/
	static void OnRotateMirrorVideo(uint32_t playerId, int angle);

	/** 停止镜像播放
	*
	*     停止镜像播放会话，当用户停止镜像投屏或由镜像投屏切换到URL播放等场景会触发该接口。
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @return    无
	*     @note   应用需要释放该会话相关资源。
	*/
	static void OnStopMirrorPlayer(uint32_t playerId);

	/** 开始纯音频播放
	*
	*     开始镜像播放会话，当用户发起镜像投屏或URL播放回到镜像播放等场景会触发该接口。
	*     @param playerId  播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param ip 发射端设备的IP地址
	*     @param device_model 发射端设备的设备型号，如iphone6
	*     @param device_name 发射端设备的设备名称
	*     @return    eg:@retval 0:应用层开始播放成功 @retval other:应用层播放失败，SDK内部会结束该会话。
	*     @note   应用需要记住playerId，由playerId参数来维护播放会话，后续相关接口都通过该字段进行关联。
	*/
	static int OnStartAudioPlayer(uint32_t playerId, const char* ip, const char* device_model, const char* device_name);

	/** 通知应用音频播放数据
	*
	*     纯音频投屏过程中，SDK收到音频数据后会进行解码，并调用该接口通知应用层已经解码的PCM音频数据。
	*     @param playerId 播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param p_src   指向视频数据快的指针
	*     @param size    视频数据快的长度
	*     @param ptsValue 视频数据对应的时间戳
	*     @return Copied size. (unit: byte)
	*     @note   应用收到音频数据后因进行解码并播放，同时需要根据时间戳与音频做同步控制。
	*/
	static uint32_t OnAudioData(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue);

	static void RefreshCoverArtFromBuffer(uint32_t playerId, const uint8_t *p_src, int size);

	static void SetVolume(uint32_t playerId, int volumePercent);

	/** 停止纯音频播放
	*
	*     停止镜像播放会话，当用户停止镜像投屏或由镜像投屏切换到URL播放等场景会触发该接口。
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @return    无
	*     @note   应用需要释放该会话相关资源。
	*/
	static void OnStopAudioPlayer(uint32_t playerId);

	/** 通知音乐播放时音频数据的格式
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param bitsdep   bitsdep of audio, 16 is default.
	* @param channels  channels of audio, 2 is default.
	* @param sampleRate sampleRate of audio, 44100 is default. .
	* @return          void
	*/
	static void NotifyAudioCodecInfo(uint32_t playerId, int32_t bitsdep, int32_t channels, int32_t sampleRate);

    static void RefreshTrackInfo(uint32_t playerId, const char* album, const char* title, const char* artist);

	/** 开始URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param url	   要播放的视频URL
	* @return    eg:@retval 0:应用层开始播放成功 @retval other:应用层播放失败，SDK内部会结束该会话。
	*/
	static int32_t OnStartVideoPlayback(uint32_t playerId, const char *ip, const char *url);

	/** 结束URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return    void
	* @note   应用应当释放播放器等相关资源
	*/
	static void OnStopVideoPlayback(uint32_t playerId);

	/** 结束URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return    void
	* @note   应用应当释放播放器等相关资源
	*/
	static void OnPauseVideoPlayback(uint32_t playerId);
	static void OnResumeVideoPlayback(uint32_t playerId);
	static void OnSeekVideoBySec(uint32_t playerId, int64_t position);
	static int32_t OnGetVideoPositionMSec(uint32_t playerId);
	static int32_t OnGetVideoDurationMSec(uint32_t playerId);
    static int32_t OnGetVideoPlayerStatus(uint32_t playerId);


    /**开始播放
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param ip 发射端设备的IP地址
	* @return    eg:@retval 0:应用层开始播放成功 @retval other:应用层播放失败，SDK内部会结束该会话。
	* @note  ios9以前系统支持
	*/
	static int32_t OnStartPhotoPlayer(uint32_t playerId, const char* ip);

	/**开始播放
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param picData, 指向图片数据的指针
	* @param size, 数据的大小
	* @return void
	* @note ios9以前系统支持
	*/
	static void OnPlayPhoto(uint32_t playerId, const uint8_t* picData, int size);

	/**结束播放
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return void
	* @note ios9以前系统支持
	*/
	static void OnStopPhotoPlayer(uint32_t playerId);


    static void OnShowPinCode(const char* ip,const char* pincode);

	/**PIN码验证成功
	* @param ip
	* @return void
	* @note
	*/
	static void OnVerifyPinSuccess(const char* ip);


    static void OnProbePlayerAbility(const char* ip, BJAirPlayAbility* const ability);
};

