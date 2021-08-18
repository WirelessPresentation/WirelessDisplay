package com.bjnet.airplaydemo.view.surfaceview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bjnet.airplaydemo.CastManager;
import com.bjnet.airplaydemo.DemoApplication;
import com.bjnet.airplaydemo.R;
import com.bjnet.airplaydemo.base.MediaChannelCtx;
import com.bjnet.airplaydemo.event.CoverArtEvent;
import com.bjnet.airplaydemo.event.TrackInfoEvent;
import com.bjnet.airplaydemo.imp.AirplayAudioChannel;
import com.bjnet.airplaydemo.view.BaseView;
import com.bjnet.cbox.module.MediaChannel;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AirAudioView extends BaseView {

    private MediaChannel channel;
    Context context;

    public AirAudioView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AirAudioView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AirAudioView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(getResources().getColor(R.color.dev_back));
        setClipChildren(true);
    }

    @Override
    public MediaChannel getChannel() {
        return channel;
    }

    @Override
    public int getChannelId() {
        return channel.getChannelId();
    }

    FrameLayout.LayoutParams textureParams;
    TextView textView, textView1, textView2;
    ImageView image;

    @Override
    public void onCreate(MediaChannel mediaChannel) {
        channel = mediaChannel;
        DemoApplication.APP.getEventBus().register(this);
        CastManager.getMgr().updateChannelFlag(getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_CREATED_MASK);

        MediaChannelCtx ctx = CastManager.getMgr().getChannelCtxById(getChannelId());
        if (ctx == null){
            Log.e(DemoApplication.TAG, "onCreate:" );
            onDestroy();
            return;
        }else{
            if ((ctx.getChannelMask() & MediaChannelCtx.CHANNEL_CLOSED_MASK) == MediaChannelCtx.CHANNEL_CLOSED_MASK){
                Log.i(DemoApplication.TAG, "onCreate: channel has closed now channel:"+ctx.toString());
                onDestroy();
                return;
            }
        }

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.color.dev_back);
        textureParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        textureParams.gravity = Gravity.CENTER;
        imageView.setLayoutParams(textureParams);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        textView = new TextView(context);
        textView1 = new TextView(context);
        textView2 = new TextView(context);
        image = new ImageView(context);
        linearLayout.addView(image);
        linearLayout.addView(textView);
        linearLayout.addView(textView2);
        linearLayout.addView(textView1);
        LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        linearLayout.setLayoutParams(layoutParams);

        addView(imageView);
        addView(linearLayout);
    }

    @Override
    public void setDisplayViewCount(int count) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackInfoEvent(TrackInfoEvent event) {
        textView.setText(event.getTitle());
        textView.setTextColor(Color.WHITE);
        textView1.setText(event.getAlbum());
        textView1.setTextColor(Color.WHITE);
        textView2.setText(event.getArtist());
        textView2.setTextColor(Color.WHITE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCoverArtEvent(CoverArtEvent event) {

        Bitmap bitmap = BitmapFactory.decodeByteArray(event.getBuffer(),0,event.getLen());
        image.setImageBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        DemoApplication.APP.getEventBus().unregister(this);
        CastManager.getMgr().updateChannelFlag(getChannelId(), MediaChannelCtx.CHANNEL_ACTIVITY_DESTROYED_MASK);
        destroyDrawingCache();
    }
}
