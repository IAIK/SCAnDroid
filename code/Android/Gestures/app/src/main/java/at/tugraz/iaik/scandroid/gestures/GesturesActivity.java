package at.tugraz.iaik.scandroid.gestures;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class GesturesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestures);
        getSupportActionBar().setTitle(getClass().getSimpleName());
    }
}
