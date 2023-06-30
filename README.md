# DriveSafe
An app that prevent drivers from falling asleep while driving. 

# Technologies used
Android, Kotlin, Java, C, C++, Google ML (Machine Learning) kit, Face detection, CameraX, Camera1, USB and UVC Camera

# How to use

![1](https://github.com/BekhruzDev/DriveSafeNew/assets/88543783/bec5b164-5a30-444f-b73f-75198b3e50ea)

This is the off state of the detector. Before turning it on, click on the "Preview" button. Place and adjust your phone properly inside your car, and make sure that the phone's front camera is pointed to your face. 
After your phone is set in a proper position, you can click on the animating power button to run the detector.

![image](https://github.com/BekhruzDev/DriveSafeNew/assets/88543783/41d0a1b8-5d45-4827-9b90-38aebfe63601)

Once sleepiness is detected, the app plays sounds that wakes the driver up. 

NOTE: Even if the driver wake up the playing sound does not stop immediately, app still keeps playing and stops about 10-20 seconds later in order to make sure that the driver is completely awake.

While detection is running, if you want to save phone battery life, click on the "Eco mode" button and detection continues running in the background.

If you prefer using external web camera, then click on the "USB Camera" button and plug in the USB Camera using (OTG) adapter. This feature is currently in TEST mode and therefore smartphones with Android 10 and below support this feature.

Driver can choose which sound the app plays
4. If flashlight option is turned on, flashlight also keeps blinking once detected sleepiness.
5. With sleep timeout option in the settings, defines how many seconds after the driver falls asleep, the app should start alarm
6. 
