# Screenshotter
A library to take screenshots without root access

## Usage - 
```
Screenshotter.getInstance()
             .setSize(720, 1280)
             .takeScreenshot(this, resultCode, data, new ScreenshotCallback() {
                 @Override
                 public void onScreenshot(Bitmap bitmap) {
                     //Enjoy your bitmap
                 }
             });
```
