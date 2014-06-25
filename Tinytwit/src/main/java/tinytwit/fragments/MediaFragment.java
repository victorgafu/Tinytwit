package tinytwit.fragments;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;

import tinytwit.activities.MainActivity;
import tinytwit.activities.R;
import tinytwit.ui.SimpleToast;

/**
 * Fragment of the ImageView created if in the tweet exists a media file
 *
 * Created by Victor on 04/06/2014.
 */
public class MediaFragment extends Fragment {

    //UI
    ImageView imageViewPicture;

    String mediaPath;
    SimpleToast toaster;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mediaPath = getArguments().getString("media");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);
        imageViewPicture = (ImageView)view.findViewById(R.id.media);
        toaster = new SimpleToast(view.getContext());
        loadImage();
        return view;
    }

    /**
     *Shows the image
     *
     */
    private void loadImage() {
        // Try to load the image.
        try {
            String filename = mediaPath;
            File file = new File(getActivity().getFilesDir() + MainActivity.MEDIA_FOLDER,
                    filename);
            Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
            imageViewPicture.setImageBitmap(image);

        } catch (Exception e) {
            // If there's an error show it.
            toaster.toast(R.string.error_create_url);
        }
    }
}
