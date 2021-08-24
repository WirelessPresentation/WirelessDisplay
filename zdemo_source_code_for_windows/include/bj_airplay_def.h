
#ifndef BJ_AIRPLAY_DEF_H
#define BJ_AIRPLAY_DEF_H

typedef enum
{
	BJ_AIRPLAY_ERROR_SUCCESS = 0,		   ///鎴愬姛
	BJ_AIRPLAY_ERROR_PORT_BINDERROR,
	BJ_AIRPLAY_ERROR_INIT_FAILED,
	BJ_AIRPLAY_ERROR_INIT_RAOP_FAILED,
	BJ_AIRPLAY_ERROR_BUTTON
}BJAirplayErrorCode;

typedef enum
{
    BJ_PLAYER_STATUS_LOADING = 0,    //正在加载
    BJ_PLAYER_STATUS_PLAYING,    //正在播放
    BJ_PLAYER_STATUS_PAUSED,     //暂停状态
    BJ_PLAYER_STATUS_ENDED,      //播放结束
    BJ_PLAYER_STATUS_FAILED,     //播放失败
}BJAirPlayerStatus;

#endif
