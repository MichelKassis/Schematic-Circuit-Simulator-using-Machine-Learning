package ca.mcgill.schematicreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.NumberFormat;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import ca.mcgill.schematicreader.model.ACResult;
import ca.mcgill.schematicreader.model.AnalysisDetail;
import ca.mcgill.schematicreader.model.electriccircuit.Circuit;
import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;

import static ca.mcgill.schematicreader.ACAnalysisActivity.ANALYSIS_DETAIL;
import static ca.mcgill.schematicreader.ProcessingActivity.ELEMENT_LIST;

public class AnalysisGraphActivity extends AppCompatActivity {

    private ArrayList<CircuitElement> mElementList;
    private AnalysisDetail mAnalysisDetail;
    private Circuit mCircuit;

    private ProgressBar mProgressBar;
    private GraphView mGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_graph);

        Intent intent = getIntent();
        mElementList = intent.getParcelableArrayListExtra(ELEMENT_LIST);
        mAnalysisDetail = intent.getParcelableExtra(ANALYSIS_DETAIL);

        mCircuit = new Circuit(mElementList);

        mProgressBar = findViewById(R.id.analysis_progress);
        mGraph = findViewById(R.id.graph);

        new Thread(this::runSimulation).start();

        // set manual X bounds
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(mAnalysisDetail.getStartFrequency());
        mGraph.getViewport().setMaxX(mAnalysisDetail.getEndFrequency());

        // set manual Y bounds
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0);

        mGraph.getViewport().setScrollable(true); // enables horizontal scrolling
        mGraph.getViewport().setScrollableY(true); // enables vertical scrolling
        mGraph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        mGraph.getViewport().setScalableY(true); // enables vertical zooming and scrolling
    }

    private void runSimulation() {
        ACResult[] acResults = mCircuit.do_AC_simulation(
                mAnalysisDetail.getStartFrequency(),
                mAnalysisDetail.getEndFrequency(),
                mAnalysisDetail.getSteps(),
                false);

        double maxY = Double.MIN_VALUE;
        int nodeOfInterest = mAnalysisDetail.getNode();

        DataPoint[] points = new DataPoint[acResults.length];
        for (int i = 0; i < acResults.length; i++) {
            if (acResults[i].vector.length <= nodeOfInterest) {
                points[i] = new DataPoint(acResults[i].frequency, 0);
            } else {
                if (acResults[i].vector[nodeOfInterest].abs() > maxY)
                    maxY = acResults[i].vector[nodeOfInterest].abs();
                points[i] = new DataPoint(acResults[i].frequency, acResults[i].vector[nodeOfInterest].abs());
            }
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points);
        double finalMaxY = maxY;
        runOnUiThread(() -> {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumFractionDigits(2);
            nf.setMinimumIntegerDigits(1);

            mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(null, nf));
            mGraph.addSeries(series);
            mGraph.getViewport().setMaxY(finalMaxY);
            mProgressBar.setVisibility(View.INVISIBLE);
        });
    }
}
