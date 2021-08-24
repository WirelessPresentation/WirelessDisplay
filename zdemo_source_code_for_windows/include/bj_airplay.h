

#ifndef BJ_AIRPLAY_ITF_H_
#define BJ_AIRPLAY_ITF_H_

#include <stdint.h>
#include <stdarg.h>

#include <jni.h>

#ifdef BJ_AIRPLAY_EXPORTS
#define BJ_AIRPLAY_API
#else
#define BJ_AIRPLAY_API __attribute__((visibility ("default")))
#endif

//BJ_AIRPLAY_FLAGS
#define BJ_AIRPLAY_SUPPORT_ROTATION_FLAG (0x00000001)
#define BJ_AIRPLAY_SUPPORT_URL_FLAG (0x00000002)
#define BJ_AIRPLAY_SUPPORT_MBNAUL_FLAG (0x00000004)
#define BJ_AIRPLAY_DISABLE_MBNAUL_FLAG (0x00000008)
#define BJ_AIRPLAY_SUPPORT_SPSPPSNAUL_FLAG (0x00000010)


typedef enum
{
    BJAir_Resolution_1080P = 0,
    BJAir_Resolution_720P = 1,
    BJAir_Resolution_800x600 = 2,
    BJAir_Resolution_1600x900 = 3,
    BJAir_Resolution_2560x1440 = 4,
    BJAir_Resolution_3840x2160 = 5,
    BJAir_Resolution_Button
}BJAirResolutionType;

typedef enum
{
    BJAir_RepublishType_Normal = 0,
    BJAir_RepublishType_Remove_And_Publish = 1,
    BJAir_RepublishType_Button
}BJAirRepublishType;

typedef struct BJAirPlayAbility
{
    int maxFPS;
    int resolution_type; //取值范围参考BJAirResolutionType
}BJAirPlayAbility;

typedef enum {
    BJAir_LOG_CRIT = 1,
    BJAir_LOG_ERROR,
    BJAir_LOG_WARN,
    BJAir_LOG_INFO,
    BJAir_LOG_DEBUG,
    BJAir_LOG_DETAIL,
    BJAir_LOG_MAX,
}BJAirplayLogLevel;


typedef struct {
	/** 开始镜像投屏播放
	*
	*     开始镜像播放会话，当用户发起镜像投屏或URL播放回到镜像播放等场景会触发该接口。
	*     @param playerId  播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param ip 发射端设备的IP地址
	*     @param device_model 发射端设备的设备型号，如iphone6
	*     @param device_name 发射端设备的设备名称
	*     @return    eg:@retval 0:应用层开始播放成功 @retval other:应用层播放失败，SDK内部会结束该会话。
	*     @note   应用需要记住playerId，由playerId参数来维护镜像会话，后续相关接口都通过该字段进行关联。
	*/
	int32_t (*OnStartMirrorPlayer)(uint32_t playerId, const char* ip, const char* device_model, const char* device_name);

	/**
	* \brief           Write mirror audio PCM frame to player.
	* \param bitsdep   bitsdep of audio, 16 is default.
	* \param channels  channels of audio, 2 is default.
	* \param sampleRate sampleRate of audio, 44100 is default. .
	* \return          void
	*/
	void (*NotifyMirrorAudioCodecInfo)(uint32_t playerId, int32_t bitsdep, int32_t channels, int32_t sampleRate);

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
	uint32_t(*OnMirrorAudioData)(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue);

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
	uint32_t(*OnMirrorVideoData)(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue);

	/** 镜像播放视频旋转角度通知
	*
	*     当IPAD/IPHONE等终端
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param angle 视频旋转角度，可能取值范围为0,90,180,270。所有角度为顺时针方向角度。
	*     @return    无
	*     @note   当应用收到该事件后，播放器需要根据该角度来处理旋转，才能正确显示视频。
	*/
	void(*OnRotateMirrorVideo)(uint32_t playerId, int angle);

	/** 停止镜像播放
	*
	*     停止镜像播放会话，当用户停止镜像投屏或由镜像投屏切换到URL播放等场景会触发该接口。
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @return    无
	*     @note   应用需要释放该会话相关资源。
	*/
	void(*OnStopMirrorPlayer)(uint32_t playerId);


	/** 设置音量
	*
	*     停止镜像播放会话，当用户停止镜像投屏或由镜像投屏切换到URL播放等场景会触发该接口。
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param volumePercent,音量调节,0:静音 100:音量最高
	*     @return    无
	*     @note   应用需要释放该会话相关资源。
	*/
	void(*SetVolume)(uint32_t playerId, int volumePercent);


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
	int32_t(*OnStartAudioPlayer)(uint32_t playerId, const char* ip, const char* device_model, const char* device_name);

	/** 通知音频播放时的封面图片
	*
	*     音频播放时，当前播放的音乐的封面图片，JPEG格式。
	*     @param playerId  播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @param p_src JPEG图片内容
	*     @param size JPEG图片内容长度
	*     @note   应用应该将图片内容做持久化的文件并进行呈现。android暂未实现
	*/
	void(*RefreshCoverArtFromBuffer)(uint32_t playerId, const uint8_t *p_src, int size);



    /** 通知音频播放时的album,title,artist
     * \brief         Refresh audio ID3 information on UI.
     * The function may not be necessary.
     *
     * \param album   The audio album.
     * \param title   The audio title.
     * \param artist  The audio artist.
     * \return        void
     */
    void(*RefreshTrackInfo)(uint32_t playerId, const char* album, const char* title, const char* artist);


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
	uint32_t(*OnAudioData)(uint32_t playerId, const uint8_t *p_src, uint32_t size, int64_t ptsValue);


	/** 停止纯音频播放
	*
	*     停止镜像播放会话，当用户停止镜像投屏或由镜像投屏切换到URL播放等场景会触发该接口。
	*     @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	*     @return    无
	*     @note   应用需要释放该会话相关资源。
	*/
	void(*OnStopAudioPlayer)(uint32_t playerId);

	/** 通知音乐播放时音频数据的格式
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param bitsdep   bitsdep of audio, 16 is default.
	* @param channels  channels of audio, 2 is default.
	* @param sampleRate sampleRate of audio, 44100 is default. .
	* @return          void
	*/
	void(*NotifyAudioCodecInfo)(uint32_t playerId, int32_t bitsdep, int32_t channels, int32_t sampleRate);

	/** 开始URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param url	   要播放的视频URL
	* @return    eg:@retval 0:应用层开始播放成功 @retval other:应用层播放失败，SDK内部会结束该会话。
	*/
	int32_t(*OnStartVideoPlayback)(uint32_t playerId, const char* ip, const char *url);

	/** 结束URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return    void
	* @note   应用应当释放播放器等相关资源
	*/
	void(*OnStopVideoPlayback)(uint32_t playerId);

	/** 结束URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return    void
	* @note   应用应当释放播放器等相关资源
	*/
	void(*OnPauseVideoPlayback)(uint32_t playerId);

	/** 继续播放URL视频播放会话
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return    void
	* @note   暂停后继续播放
	*/
	void(*OnResumeVideoPlayback)(uint32_t playerId);

	/** 播放URL视频播放Seek
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param position_sec，进度条拖动的位置，单位秒
	* @return    void
	* @note   进度条拖动到第几秒播放
	*/
	void(*OnSeekVideoBySec)(uint32_t playerId, int64_t position_sec);

	/** 获取播放器播放URL当前所处的位置
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return int32_t 当前播放器所播放到的位置，单位为毫秒
	* @note   返回当前播放器所播放到的位置，单位为毫秒
	*/
	int32_t(*OnGetVideoPositionMSec)(uint32_t playerId);

    /** 获取播放器当前的播放状态
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return int32_t 取值范围参考BJAirPlayerStatus
	* @note
	*/
    int32_t(*OnGetVideoPlayerStatus)(uint32_t playerId);

	/** 获取URL的总时长
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return int32_t 获取URL的总时长，单位为毫秒
	* @note
	*/
	int32_t(*OnGetVideoDurationMSec)(uint32_t playerId);

	/**开始播放
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param ip 发射端设备的IP地址
	* @return    eg:@retval 0:应用层开始播放成功 @retval other:应用层播放失败，SDK内部会结束该会话。
	* @note  ios9以前系统支持
	*/
	int32_t(*OnStartPhotoPlayer)(uint32_t playerId, const char* ip);

	/**开始播放
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @param picData, 指向图片数据的指针
	* @param size, 数据的大小
	* @return void
	* @note ios9以前系统支持
	*/
	void(*OnPlayPhoto)(uint32_t playerId, const uint8_t* picData, int size);

	/**结束播放
	* @param playerId，播放会话ID(该ID由SDK内部统一分配，全局唯一)
	* @return void
	* @note ios9以前系统支持
	*/
	void(*OnStopPhotoPlayer)(uint32_t playerId);

	/**显示PIN码
	* @param ip
	* @param pincode
	* @return void
	* @note
	*/
	void(*OnShowPinCode)(const char* ip,const char* pincode);

	/**PIN码验证成功
	* @param ip
	* @return void
	* @note
	*/
	void(*OnVerifyPinSuccess)(const char* ip);

    /**当某个发射端发起投屏时，SDK回调该接口，获取最大帧率和分辨率参数
	* @param ip
	* @return void
	* @note
	*/
    void(*OnProbePlayerAbility)(const char* ip, BJAirPlayAbility* const ability);

    /**用户自定义的日志函数接口
	* @param level
	* @return void
	* @note
	*/
    void(*LogFun)(unsigned int level, const char *format,  va_list argp);
} BJAirplayFunctionCbs_t;

/**
* BJAirplay Sdk的初始化结构体
*/
typedef struct
{
	char friendly_name[128];           ///Airplay接收端的显示名称，即在iPhone/IPad等设备搜索到的Airplay接收端的名称
	int  framerate;                    ///取值范围15-60; >60会设置为60  <15会设置为15
	int  resolution_type;              ///resolution_type 取值范围参考BJAirResolutionType
	char password[256];				   ///投屏密码
	char licenseKey[1024];			   ///License信息，从License文件中读取
	//char itf_name[32];                 ///从该接口获取MAC地址
	char mac_addr[32];                 ///格式:00:26:b9:52:7b:e0
    int airplay_flags;			       ///参考BJ_AIRPLAY_FLAGS定义，位域类型
	int max_session_nums;              ///最大nums 取值范围1-9
	int republish_mode;                ///BJAir_RepublishType_Normal:normal repubsh BJAir_RepublishType_Remove_And_Publish:remove service and publish 取值范围参考:BJAirRepublishType
    int log_level;                     ///日志级别,取值范围参考BJAirplayLogLevel
    BJAirplayFunctionCbs_t callback;   ///用户回调接口，需要由用户看来实现
}AirplayInitPara;

typedef void(*UserDefinePrint)(const char* message);

#ifdef  __cplusplus
extern "C" {
#endif

/** 初始化BJAirplay SDK
*
*     初始化BJAirplay SDK。
*     @param JNIEnv *env:应用传入env,sdk 会读需env使用设备的硬件信息
*     @param initpara 初始化结构体
*     @return    eg:@retval 0 BJAirplay SDK初始化成功 @retval other: BJAirplay SDK初始化失败。
*     @note   应用需要释放该会话相关资源。
*/
BJ_AIRPLAY_API int  InitBJAirplay(JNIEnv *env,AirplayInitPara* initpara);

/** 设置BJAirplay 设置投屏密码
*
*     @param password
*     @note   调用该接口必须保证当前没有任何Airplay会话存在,否则返回-1
*     @ret   0:success -1:not support
*/
BJ_AIRPLAY_API int SetAirplayPassword(const char *password);

/** 设置BJAirplay 修改airplay名称
*
*     @param name
*/
BJ_AIRPLAY_API void ServiceRename(const char* name);

/** 设置BJAirplay republish airplay服务
*
*     @param name
*/
BJ_AIRPLAY_API void ServiceRepublish();


/** 强制退出某一路Airplay播放
*
*     强制退出某一路Airplay播放
*     @param playerId 播放会话ID
*     @note   SDK内部会将airplay会话强制结束
*/
BJ_AIRPLAY_API void  KickOut(uint32_t playerId);

BJ_AIRPLAY_API void UninitBJAirplay();

/** 动态设置日志级别
*
*     动态设置日志级别
*     @param level 取值范围参考BJAirplayLogLevel
*     @note
*/

BJ_AIRPLAY_API void SetAirplayLogLevel(int level);

#ifdef  __cplusplus
}
#endif

#endif
