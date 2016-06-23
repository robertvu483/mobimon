package com.alarmnotification.mobimon;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.HashMap;

import Object.GlobalContants;
import Object.Equipment;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnClickListener {
    private ImageButton[] navs;
    private ProgressBar hpBar;
    private HashMap<Integer, ImageView> petParts;
    private Handler hpTick;
    private Runnable hpDrop;
    private int curHp;
    private PendingIntent alarmIntent;
    private boolean alarmState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        alarmState = GlobalContants.SET_ALARM_ENABLED;

        int[] navIds = new int[] { R.id.petNav, R.id.bagNav, R.id.feedingNav, R.id.fightNav, R.id.facebookNav };
        this.navs = new ImageButton[navIds.length];
        for (int i = 0, len = navIds.length; i < len; i++) {
            this.navs[i] = (ImageButton)this.findViewById(navIds[i]);
            this.navs[i].setOnClickListener(this);
        }

        this.hpBar = (ProgressBar)findViewById(R.id.hpBar_home);

        int[] petPartIds = this.getPetPartIds();
        this.petParts = new HashMap<>();
        for (int i = 0, len = petPartIds.length; i < len; i++) {
            this.petParts.put(petPartIds[i], (ImageView)this.findViewById(petPartIds[i]));
        }

        setImageViewSet();

        hpTick = new Handler();
        hpDrop = new Runnable() {
            @Override
            public void run() {
                if (--curHp <= 0) {
                    resetGame();
                } else {
                    hpTick.postDelayed(this, GlobalContants.HP_DROP_INTERVAL);
                    hpBar.setProgress(curHp);
                }
            }
        };

        Context context = getApplicationContext();
        Intent intent = new Intent(context, HungerReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, GlobalContants.HUNGER_ALARM, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private int[] getPetPartIds() {
        return new int[] { R.id.head_home, R.id.body_home, R.id.foot_home, R.id.wing_home };
    }

    private void calculateFromLastTime() {
        SharedPreferences data = getSharedPreferences(GlobalContants.USER_PREF, MODE_PRIVATE);
        long cur = System.currentTimeMillis();
        long startTime = data.getLong(GlobalContants.START_TIME, 0);
        curHp = data.getInt(GlobalContants.CUR_HP, 100);
        curHp -= (cur - startTime) / GlobalContants.HP_DROP_INTERVAL - (data.getLong(GlobalContants.LAST_ACTIVE, 0) - startTime) / GlobalContants.HP_DROP_INTERVAL;

        if (curHp <= 0) {
            resetGame();
        } else {
            long timeLeft = GlobalContants.HP_DROP_INTERVAL - (cur - startTime) % GlobalContants.HP_DROP_INTERVAL;
            hpBar.setProgress(curHp);
            hpTick.postDelayed(hpDrop, timeLeft);
        }
    }

    private void resetGame() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sorry!")
                .setMessage("Your pet died due to starvation. Your game has been reset, all your item has been gone. You are now raising a new pet!")
                .setPositiveButton("I understood", this)
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void setImageViewSet() {
        // TODO
        int[] petPartIds = getPetPartIds();

        // ...
        Equipment[] temp = new Equipment[4];
        temp[0] = new Equipment();
        temp[0].setLargeImage(BitmapFactory.decodeResource(getResources(), R.drawable.head1));
        temp[1] = new Equipment();
        temp[1].setLargeImage(BitmapFactory.decodeResource(getResources(), R.drawable.body1));
        temp[2] = new Equipment();
        temp[2].setLargeImage(BitmapFactory.decodeResource(getResources(), R.drawable.foot1));
        temp[3] = new Equipment();
        temp[3].setLargeImage(BitmapFactory.decodeResource(getResources(), R.drawable.wing1));
        // ...

        for (int i = 0, len = petPartIds.length; i < len; i++) {
            petParts.get(petPartIds[i]).setImageBitmap(temp[i].getLargeImage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.petNav:
                this.goToActivity(PetActivity.class);
                break;
            case R.id.bagNav:
                goToActivity(BagActivity.class);
                break;
            case R.id.feedingNav:
                this.goToActivity(FeedingActivity.class);
                break;
            case R.id.fightNav:
                break;
            case R.id.facebookNav:
                break;
        }
    }

    private void goToActivity(Class<? extends Activity> clazz) {
        Intent intent = new Intent(this.getApplicationContext(), clazz);
        setAlarm();
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        calculateFromLastTime();
        AlarmManager alarmMgr = (AlarmManager)getApplicationContext().getSystemService(ALARM_SERVICE);
        if (alarmMgr!= null) {
            alarmMgr.cancel(alarmIntent);
            alarmState = GlobalContants.SET_ALARM_DISABLED;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        setAlarm();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SharedPreferences data = getSharedPreferences(GlobalContants.USER_PREF, MODE_PRIVATE);
        data.edit()
                .putLong(GlobalContants.START_TIME, System.currentTimeMillis())
                .commit();

        // TODO: setDefaultSet()

        curHp = 100;
        hpBar.setProgress(curHp);
        hpTick.postDelayed(hpDrop, GlobalContants.HP_DROP_INTERVAL);
        setImageViewSet();
    }

    private void setAlarm() {
        if (alarmState == GlobalContants.SET_ALARM_DISABLED) {
            hpTick.removeCallbacks(hpDrop);

            long cur = System.currentTimeMillis();
            int healthState = GlobalContants.FULL_HEALTH;
            SharedPreferences data = getSharedPreferences(GlobalContants.USER_PREF, MODE_PRIVATE);
            long timeLeft = GlobalContants.HP_DROP_INTERVAL - (cur - data.getLong(GlobalContants.START_TIME, 0)) % GlobalContants.HP_DROP_INTERVAL;

            if (curHp > 50) {
                timeLeft += (curHp - 51) * GlobalContants.HP_DROP_INTERVAL;
            } else if (curHp > 20){
                timeLeft += (curHp - 21) * GlobalContants.HP_DROP_INTERVAL;
                healthState = GlobalContants.HALF_HEALTH;
            } else {
                timeLeft += (curHp - 1) * GlobalContants.HP_DROP_INTERVAL;
                healthState = GlobalContants.NO_HEALTH;
            }

            data.edit()
                    .putLong(GlobalContants.LAST_ACTIVE, cur)
                    .putInt(GlobalContants.CUR_HP, curHp)
                    .putInt(GlobalContants.HEALTH_STATE, healthState)
                    .commit();

            ((AlarmManager)getApplicationContext().getSystemService(ALARM_SERVICE)).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeLeft, alarmIntent);
            alarmState = GlobalContants.SET_ALARM_ENABLED;
        }
    }
}
