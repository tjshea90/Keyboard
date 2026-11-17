# If the Swoop keyboard misbehaves (plain words, for the owner)

Swoop is a keyboard like Gboard: Android only uses it while it is switched on AND chosen. Your old keyboard stays installed until you decide to remove it, so you can always go back. You do not lose anything the keyboard learned by switching away.

## I cannot type / the keyboard does not show
1. Tap the text box once more. If a keyboard icon appears at the bottom corner (a small keyboard symbol near the navigation buttons), tap it and pick your old keyboard (for example Gboard or the stock one).
2. No icon? Pull down the notification shade: Android often shows "Choose input method". Tap it and pick the old keyboard.
3. Still nothing: Settings > System > Languages & input > On-screen keyboard > Manage keyboards (the words differ a little on the tablet's modified Android). Switch Swoop OFF and your old keyboard ON.
4. Last resort: restart the tablet. Android falls back to the stock keyboard when the chosen one cannot start.

## Swoop shows a plain, simple keyboard (the "safe layout")
That is on purpose: if the keyboard crashes three times within a minute it turns off the clever parts (glide, suggestions, learning) and keeps only typing, so you are never locked out. Tap the globe key (bottom left), or open the Swoop app and press "Go back to my previous keyboard" (it opens Android's keyboard picker), or tell Claude what you did just before it happened. The Swoop app's Diagnostics screen says "safe layout: ON" while this is active; it turns itself off the first time the keyboard starts and draws normally.

## It is slow or it guesses badly
Open the Swoop app > Diagnostics > Copy, and paste what it copies into the chat. It contains only counters and timings, never what you typed. Do NOT paste the learning file or a screenshot of private text.

## I want to start the learning over
Swoop app > Forget everything the keyboard learned (arrives with KB-206; until then, clear the app's storage in Android's Settings > Apps > Swoop > Storage). Your exported backup files in Downloads are not touched.

## I want my learning back after reinstalling
Before uninstalling, use Export in the Swoop app (it writes a file to Downloads). After installing again, use Import and pick that file. An update installed from a Release link keeps everything; only a different signing key forces an uninstall, which is why Swoop's key is created once and protected.

(Written at KB-004, updated at KB-106: the safe layout, the globe key on the bottom row and the "Go back to my previous keyboard" button on the Swoop screen now exist. The globe key switches to the next keyboard; if Android will not, it shows the keyboard picker. Importing the browser's file is KB-205.)
