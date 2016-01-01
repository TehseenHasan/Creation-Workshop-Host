Creation-Workshop-Host
======================

What does CWH look like?
-------------------------------------------------------------------------------  
Old Version Looks like [this](https://github.com/area515/Creation-Workshop-Host/blob/master/host/images/cwh.png).  
The New Version Looks like [this](https://github.com/area515/Creation-Workshop-Host/blob/master/host/images/cwhNew.png).  
Another GUI looks like [this](https://github.com/area515/Creation-Workshop-Host/blob/master/host/images/anotherGui.png).  
And yet another GUI is in the works if you don't like the above three.  

Where are the instructions for installing on the Raspberry Pi?  
-------------------------------------------------------------------------------  
[Here](https://github.com/area515/Creation-Workshop-Host/wiki/Raspberry-Pi-Manual-Setup-Instructions).

Where is a video of how to use CWH with CWS and Zip files?  
-------------------------------------------------------------------------------  
[Here](https://www.youtube.com/watch?v=J3HTCkxlKcw).

Where is a video of how to setup CWH from scratch on the Raspberry Pi?
-------------------------------------------------------------------------------  
[Here](https://www.youtube.com/watch?v=ng1Sj2ktWhU).

How do I use this new version of the GUI you've started?  
-------------------------------------------------------------------------------  
Change the following line:  
```
hostGUI=resources
```  
in this file:  
```
[LocationWhereCWHIsInstalled]/config.properties
```  
to this:  
```
hostGUI=resourcesnew
```  
Once the new GUI has all of the functionality of the current version, we'll use this version automatically.

What features does CWH have?
-------------------------------------------------------------------------------  
1. Print STL files without performing pre-slice stage.
2. Load STL files directly from thingiverse or the internet.
3. Uses common xml Creation Workshop configuration files.
4. Custom printer mask overlays.
5. TLS encryption with Basic authentication.
6. Use of [FreeMarker](http://freemarker.org/) templating in configuration files.
7. Restful developer API for printer management.
8. Video Recording and playback of build.
9. Print Zip/CWS files exported from Creation Workshop.
10. Execution of custom gcode from GUI.
11. Managment of multiple printers with a single print host.
12. Plugin based notification framework.
13. Sophisticated javascript calculators that compute gradients, exposure time, lift speed and distance.
14. Notification of Printer events through webSockets.
15. Simple printer setup for Zip/CWS based printing.
16. Automatic updates via online installs or manual updates through offline installs
17. [Script](https://github.com/area515/Creation-Workshop-Host/blob/master/host/bin/browseprinter.sh)(or a native Windows app)to find CWH based printers on the local network with zero network setup. (DLNA/UPNP support)
18. Capability of building printer configurations with simulated Serial ports and displays.
19. Two clicks and a password for Linux Wifi management to support a turnkey hardware solution.
20. Automatic projector model detection (1 model currently supported, others supported at request and hardware accessibility)
21. 3d firmware printer port detection.
22. Hardware compatibility test suite.
23. Experiment and override printing options after a print is already in progress.
24. Take still pictures of the build at the click of a button.
25. Print material detection framework implemented with a computer vision inspection portal.
26. Supports Windows, OSX and Linux OSs. (Service support on Linux)
27. Supports dynamic direct slice-on-the-fly 3d printing of (.mazecube, .stl, .jpg/.png/.gif, .cws/.zip) files.

Do you want to install the latest stable build?
-------------------------------------------------------------------------------
```
sudo wget https://github.com/area515/Creation-Workshop-Host/raw/master/host/bin/start.sh
sudo chmod 777 start.sh
sudo ./start.sh
```

Do you want to install the latest unstable daily development build?
-------------------------------------------------------------------------------
```
sudo wget https://github.com/WesGilster/Creation-Workshop-Host/raw/master/host/bin/start.sh
sudo chmod 777 start.sh
sudo ./start.sh WesGilster
```

Do you want to install under Windows?
------------------------------------------
* Download the latest version from: 
* [https://github.com/area515/Creation-Workshop-Host/blob/master/host/cwh-X.XX.zip](https://github.com/area515/Creation-Workshop-Host/blob/master/host/)  
 or
* [https://github.com/WesGilster/Creation-Workshop-Host/blob/master/host/cwh-X.XX.zip](https://github.com/area515/Creation-Workshop-Host/blob/master/host/)  
* Unzip the zip file into the directory of your choice.
* Double click on start.bat.

Do you want to use your web browser to automatically navigate to the running printer host without knowing anything about how your network is setup?
----------------------------------------------------------------------
* Download the latest version from:
* [https://github.com/area515/Creation-Workshop-Host/blob/master/host/cwhClient-X.XX.zip](https://github.com/area515/Creation-Workshop-Host/blob/master/host/)  
 or
* [https://github.com/WesGilster/Creation-Workshop-Host/blob/master/host/cwhClient-X.XX.zip](https://github.com/area515/Creation-Workshop-Host/blob/master/host/)  
* Unzip the zip file into the directory of your choice.
* If you are in Linux run this:
````````
	sudo browseprinter.sh
````````
If you are in windows double click this:
````````
	browseprinter.bat
````````
