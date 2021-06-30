package ca.mcgill.schematicreader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static ca.mcgill.schematicreader.ResultViewActivity.NETLIST_STRING;

public class CircuitExportActivity extends AppCompatActivity {

    private String mNetlist;
    private TextView mNetlistText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circuit_export);

        Intent intent = getIntent();
        mNetlist = intent.getStringExtra(NETLIST_STRING);

        mNetlistText = findViewById(R.id.netlist_text);
        mNetlistText.setText(mNetlist);
        mNetlistText.setEnabled(false);

        findViewById(R.id.clipboard_button).setOnClickListener((view) -> copyToClipboard());
    }

    private void copyToClipboard() {
        Context context = getApplicationContext();
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(mNetlist);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(
                            context.getResources().getString(
                                    R.string.netlist_clipboard), mNetlist);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show();
    }
}
