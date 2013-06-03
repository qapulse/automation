package com.littleinc.MessageMe.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DeviceUtil;

@TargetApi(14)
public class GroupNameActivity extends ActionBarActivity {

    private EditText groupNameInput;

    private ImageView groupImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_name);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        groupNameInput = (EditText) findViewById(R.id.group_name_input);
        groupNameInput
                .setOnEditorActionListener(new GroupEditorActionListener());

        groupImage = (ImageView) findViewById(R.id.create_group_image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.group_name_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case MessageMeConstants.NEW_GROUP_REQUEST_CODE:
            switch (resultCode) {
            case RESULT_OK:
                final long roomId = data.getLongExtra(
                        MessageMeConstants.RECIPIENT_ID_KEY, -1);

                new DatabaseTask() {

                    Room room;

                    @Override
                    public void work() {
                        room = new Room(roomId);
                        room.load();
                    }

                    @Override
                    public void done() {
                        if (mMessagingServiceRef != null) {
                            mMessagingServiceRef
                                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
                        } else {
                            LogIt.w(GroupNameActivity.class,
                                    "mMessagingServiceRef is null, cannot send INTENT_NOTIFY_CONTACT_LIST");
                        }

                        Intent intent = new Intent(GroupNameActivity.this,
                                ChatActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        intent.putExtra(MessageMeConstants.RECIPIENT_ROOM_KEY,
                                room.toPBRoom().toByteArray());

                        startActivity(intent);
                        finish();
                    }
                };
                break;
            }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            groupImage.setVisibility(View.GONE);
        } else {
            groupImage.setVisibility(View.VISIBLE);
        }

    }

    private void startGroupContactsSelection() {
        if (!TextUtils.isEmpty(groupNameInput.getText().toString().trim())) {
            Intent intent = new Intent(this, GroupContactsActivity.class);
            intent.putExtra(MessageMeConstants.GROUP_NAME_KEY, groupNameInput
                    .getText().toString().trim());

            startActivityForResult(intent,
                    MessageMeConstants.NEW_GROUP_REQUEST_CODE);
        } else {
            Toast.makeText(this, R.string.group_name_invalid,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.action_add) {
            startGroupContactsSelection();
        }

        return super.onOptionsItemSelected(item);
    }

    private class GroupEditorActionListener implements OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            if (DeviceUtil.wasActionPressed(v, actionId, event)) {
                startGroupContactsSelection();
                return true;
            } else {
                return false;
            }
        }
    }
}