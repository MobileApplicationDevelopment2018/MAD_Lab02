package it.polito.mad.lab02.barcodereader;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

class DummyProcessor extends FocusingProcessor<Barcode> {

    public DummyProcessor(Detector<Barcode> detector, Tracker<Barcode> tracker) {
        super(detector, tracker);
    }

    @Override
    public int selectFocus(Detector.Detections<Barcode> detections) {
        return -1;
    }
}
