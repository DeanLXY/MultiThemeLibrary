package com.wangx.multithemelibrary;

import android.os.Bundle;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private LayoutInflaterFactoryDelegate mFactory;
    private int theme = R.style.AppBlackTheme;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //called before super.x()
        mFactory = new LayoutInflaterFactoryDelegate();
        LayoutInflaterCompat.setFactory(LayoutInflater.from(this), mFactory);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void click(View view){
        if (theme == R.style.AppBlackTheme){
            theme = R.style.AppLightTheme;
        }else{
            theme = R.style.AppBlackTheme;
        }
        setTheme(theme);
        mFactory.changeTheme(this);
    }
}
