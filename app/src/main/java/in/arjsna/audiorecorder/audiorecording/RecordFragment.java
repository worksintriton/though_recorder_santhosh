package in.arjsna.audiorecorder.audiorecording;

import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;
import java.util.Locale;

import in.arjsna.audiorecorder.AppConstants;
import in.arjsna.audiorecorder.R;
import in.arjsna.audiorecorder.activities.PlayListActivity;
import in.arjsna.audiorecorder.activities.SettingsActivity;
import in.arjsna.audiorecorder.audiovisualization.GLAudioVisualizationView;
import in.arjsna.audiorecorder.di.qualifiers.ActivityContext;
import in.arjsna.audiorecorder.mvpbase.BaseFragment;
import in.arjsna.audiorecorder.recordingservice.AudioRecordService;
import in.arjsna.audiorecorder.recordingservice.AudioRecorder;
import in.arjsna.audiorecorder.theme.ThemeHelper;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class RecordFragment extends BaseFragment implements AudioRecordMVPView {
  private static final String LOG_TAG = RecordFragment.class.getSimpleName();
  private FloatingActionButton mRecordButton = null;
  private FloatingActionButton mPauseButton = null;
  private GLAudioVisualizationView audioVisualization;

  private TextView chronometer,voice_display;
  private boolean mIsServiceBound = false;
  private AudioRecordService mAudioRecordService;
  private ObjectAnimator alphaAnimator;
  private FloatingActionButton mSettingsButton;
  private FloatingActionButton mPlayListBtn;

  private Button controlThrouVoiceBtn;


  @Inject
  @ActivityContext
  public Context mContext;

  @Inject
  public AudioRecordPresenter<AudioRecordMVPView> audioRecordPresenter;

  public static RecordFragment newInstance() {
    return new RecordFragment();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    audioRecordPresenter.onAttach(this);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View recordView = inflater.inflate(R.layout.fragment_record, container, false);
    initViews(recordView);
    bindEvents();
    return recordView;
  }

  private void bindEvents() {
    RxView.clicks(mRecordButton)
        .subscribe(o -> audioRecordPresenter.onToggleRecodingStatus());
    RxView.clicks(mSettingsButton)
        .subscribe(o -> startActivity(new Intent(mContext, SettingsActivity.class)));
    RxView.clicks(mPlayListBtn)
        .subscribe(o -> startActivity(new Intent(mContext, PlayListActivity.class)));
    RxView.clicks(mPauseButton)
        .subscribe(o -> audioRecordPresenter.onTogglePauseStatus());
  }

  private void initViews(View recordView) {
    chronometer = recordView.findViewById(R.id.chronometer);

    voice_display = recordView.findViewById(R.id.voice_text);

    controlThrouVoiceBtn = recordView.findViewById(R.id.btnVoicegesture);

    audioVisualization = recordView.findViewById(R.id.visualizer_view);

    mSettingsButton = recordView.findViewById(R.id.settings_btn);
    mPlayListBtn = recordView.findViewById(R.id.play_list_btn);
    mRecordButton = recordView.findViewById(R.id.btnRecord);
    mPauseButton = recordView.findViewById(R.id.btnPause);
    mPauseButton.setVisibility(View.GONE); //hide pause button before recording starts

    controlThrouVoiceBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        if(audioRecordPresenter.isRecording()){

          audioRecordPresenter.onToggleRecodingStatus();

          getSpeechInput(recordView);

        }

        else {

          getSpeechInput(recordView);

        }


      }
    });

    alphaAnimator =
        ObjectAnimator.ofObject(chronometer, "alpha", new FloatEvaluator(), 0.2f);
    alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
    alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
    audioRecordPresenter.onViewInitialised();
  }

  public void getSpeechInput(View view) {

    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
      startActivityForResult(intent, 10);
    } else {
      Toast.makeText(getActivity(), "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case 10:
        if (resultCode == RESULT_OK && data != null) {
          ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
          if(result.get(0).equalsIgnoreCase("Start")){

            voice_display.setVisibility(View.GONE);

            if(!audioRecordPresenter.isRecording()){

              audioRecordPresenter.onToggleRecodingStatus();
            }

            else {

              Toast.makeText(getActivity(),"Already Recording is in Process",Toast.LENGTH_LONG).show();

            }

          }

          else if(result.get(0).equalsIgnoreCase("Pause")){

            voice_display.setVisibility(View.GONE);

            if(audioRecordPresenter.isRecording()){

              audioRecordPresenter.onTogglePauseStatus();
            }

            else {

              Toast.makeText(getActivity(),"No Recording Found",Toast.LENGTH_LONG).show();

            }


          }

          else {

            voice_display.setVisibility(View.GONE);

            if(audioRecordPresenter.isRecording()){

              audioVisualization.release();

              audioRecordPresenter.onDetach();
            }

            else {

              //Toast.makeText(getActivity(),"No Recording Found",Toast.LENGTH_LONG).show();

            }



          }

        }
        break;
    }
  }

  private void setAsPauseBtn() {
    alphaAnimator.cancel();
    chronometer.setAlpha(1.0f);
    mPauseButton.setImageResource(R.drawable.ic_media_pause);
  }

  private void setAsResumeBtn() {
    alphaAnimator.start();
    mPauseButton.setImageResource(R.drawable.ic_media_record);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    audioVisualization.release();
    audioRecordPresenter.onDetach();
  }

  @Override public void refreshTheme(ThemeHelper themeHelper) {
    GLAudioVisualizationView.ColorsBuilder colorsBuilder =
        new GLAudioVisualizationView.Builder(mContext);
    colorsBuilder.setBackgroundColor(themeHelper.getPrimaryColor());
    colorsBuilder.setLayerColors(themeHelper.getLayerColor());
    audioVisualization.updateConfig(colorsBuilder);
    chronometer.setTextColor(themeHelper.getLayerColor()[3]);
    mRecordButton.setRippleColor(themeHelper.getLayerColor()[3]);
    mSettingsButton.setRippleColor(themeHelper.getLayerColor()[3]);
    mPlayListBtn.setRippleColor(themeHelper.getLayerColor()[3]);
    mPauseButton.setRippleColor(themeHelper.getLayerColor()[3]);
  }

  @Override public void updateChronometer(String text) {
    chronometer.setText(text);
  }

  @Override public void togglePauseStatus() {
    if (audioRecordPresenter.isPaused()) {
      setAsResumeBtn();
    } else {
      setAsPauseBtn();
    }
  }

  @Override
  public void pauseRecord() {
    mAudioRecordService.pauseRecord();
    togglePauseStatus();
  }

  @Override
  public void resumeRecord() {
    mAudioRecordService.resumeRecord();
    togglePauseStatus();
  }

  @Override public void toggleRecordButton() {
    mRecordButton.setImageResource(
        audioRecordPresenter.isRecording() ? R.drawable.ic_media_stop : R.drawable.ic_media_record);
  }

  @Override public void linkGLViewToHandler() {
    audioVisualization.linkTo(mAudioRecordService.getHandler());
  }

  @Override public void setPauseButtonVisible() {
    mPauseButton.setVisibility(View.VISIBLE);
  }

  @Override public void setPauseButtonInVisible() {
    mPauseButton.setVisibility(View.GONE);
  }

  @Override public void setScreenOnFlag() {
    getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override public void clearScreenOnFlag() {
    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override public void startServiceAndBind() {
    Intent intent = new Intent(mContext, AudioRecordService.class);
    mContext.startService(intent);
    bindToService();
  }

  @Override
  public void bindToService() {
    Intent intent = new Intent(mContext, AudioRecordService.class);
    mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    registerLocalBroadCastReceiver();
  }

  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      mIsServiceBound = true;
      mAudioRecordService =
          ((AudioRecordService.ServiceBinder) iBinder).getService();
      Log.i("Tesing", " " + mAudioRecordService.isRecording() + " recording");
      audioRecordPresenter.onServiceStatusAvailable(mAudioRecordService.isRecording(),
          mAudioRecordService.isPaused());
    }

    @Override public void onServiceDisconnected(ComponentName componentName) {
    }
  };

  @Override
  public void unbindFromService() {
    unRegisterLocalBroadCastReceiver();
    if (mIsServiceBound) {
      mIsServiceBound = false;
      mContext.unbindService(serviceConnection);
    }
  }

  @Override
  public Disposable subscribeForTimer(Consumer<AudioRecorder.RecordTime> recordTimeConsumer) {
    return mAudioRecordService.subscribeForTimer(recordTimeConsumer);
  }

  private void unRegisterLocalBroadCastReceiver() {
    LocalBroadcastManager.getInstance(mContext).unregisterReceiver(serviceUpdateReceiver);
  }

  private void registerLocalBroadCastReceiver() {
    LocalBroadcastManager.getInstance(mContext)
        .registerReceiver(serviceUpdateReceiver, new IntentFilter(AppConstants.ACTION_IN_SERVICE));
  }

  @Override public void stopServiceAndUnBind() {
    Intent intent = new Intent(mContext, AudioRecordService.class);
    mContext.stopService(intent);
    unbindFromService();
  }

  private final BroadcastReceiver serviceUpdateReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      if (!intent.hasExtra(AppConstants.ACTION_IN_SERVICE)) return;
      String actionExtra = intent.getStringExtra(AppConstants.ACTION_IN_SERVICE);
      audioRecordPresenter.onServiceUpdateReceived(actionExtra);
    }
  };
}