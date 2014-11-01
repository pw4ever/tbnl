<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [TBNL: Rule your Android, as the Root, fully programmable, in Clojure ](#tbnl-rule-your-android-as-the-root-fully-programmable-in-clojure)
	- [see it in action](#see-it-in-action)
	- [try it](#try-it)
	- [annotated source in [Marginalia](https://github.com/gdeer81/marginalia)](#annotated-source-in-marginaliahttpsgithubcomgdeer81marginalia)
	- [permission](#permission)
	- [thanks](#thanks)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

TBNL: Rule your Android, as the Root, fully programmable, in Clojure 
=====
Copyright &copy; Wei Peng

The tag line above says it all. 

If you are a [hacker](https://en.wikipedia.org/wiki/Hacker_(programmer_subculture)), who wants to **own** your [rooted](http://www.androidcentral.com/root) [Android](http://www.android.com/) 4.3/SDK18+ devices in something nicer than Android's native tongue (i.e., Java) or the command-line power tools (e.g., [`am`](https://developer.android.com/tools/help/adb.html#am) and [`pm`](https://developer.android.com/tools/help/adb.html#pm)), say, [Clojure](http://clojure.org/)...

This is my gift to you.

TBNL (To Be Named Later) is a pun on the framework's open & exploratory nature, inspired by [Edi Weitz](http://weitz.de/)'s [TBNL](http://weitz.de/tbnl/).

see it in action
-----

[Video demos.](https://github.com/pw4ever/tbnl/wiki/demos)

try it
-----

First, make sure to [satisfy the build/runtime dependencies](https://github.com/pw4ever/tbnl/wiki/try-it-out#dependencies).

You may first want to try [the Figurehead Android app in Google Play Store](https://play.google.com/store/apps/details?id=figurehead.ui) on your rooted "SDK 18/Android 4.3"+ smartphones/tablets/emulators. For emulator, [Genymotion](http://www.genymotion.com/) is recommended.

To try out the full TBNL, you have 2 options:

* [download & install it *without* the source code](https://github.com/pw4ever/tbnl/wiki/try-it-out#without-source): Easy and quick to get started; but may not have the latest version; does not support further development.
* [build & install it *with* the source code](https://github.com/pw4ever/tbnl/wiki/try-it-out#with-source): Longer to set up *the first time*; but easy to build against the latest version; support further development.

In both case, ensure that:
* Have a *sole* (in the sense that [`adb`](https://developer.android.com/tools/help/adb.html) can unambiguously find it) Android device/emulator with [Android SDK 18](https://developer.android.com/about/versions/android-4.3.html) running.
 
Then, you can [run the tools](https://github.com/pw4ever/tbnl/wiki/try-it-out#running-tbnl).

All tested on [Arch Linux](https://www.archlinux.org/).

annotated source in [Marginalia](https://github.com/gdeer81/marginalia)
-----

The ultimate truth lies in the code.

This is probably what you are looking for.
* [figurehead][figurehead]: Clojure REPL on your rooted Android device plus more. 
* [messenger][messenger]: Unix-style command-line interface to your Figurehead REPL.

The other components.
* [core][core]: the shared underlying framework. 
* [mastermind][mastermind]: a single central server that brokers figureheads and C\&Cs communication (and more).
* [C\&C][cnc]: C\&C controls figurehead through mastermind.

See also [the project Wiki](https://github.com/pw4ever/tbnl/wiki).

permission
-----

I am no legal expert, and I am confused by [the plethora of Open Source licenses](https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses), so let me explain what I have in mind in plain words.

You are free to use and adapt this work in any way you want, with 2 overall principles:
* Do not hold me or any contributors liable for your actions of using this software.
* Please do the courtesy of fair acknowledgments if you derive works from it.

Please consider:
* Improving the core framework and contributing plug-ins;
* Contributing constructive criticisms: What is working, and what could be improved;
* Reporting issues and sending in patches;
* Staring this project on GitHub;
* Citing this work if you publish a paper using it;
* Letting me know if you use or extend it: I'd love to hear about your work and your use cases.

thanks
-----
* [Dr. Feng Li](http://www.engr.iupui.edu/~fengli/) and [Dr. Xukai Zou](http://cs.iupui.edu/~xkzou/), who advise and support my research into Android system security that begets TBNL.
* The Clojure community.
  * In particular, [Alexander Yakushev](https://github.com/alexander-yakushev) and others in the small but welcoming family of [Clojure on Android](http://clojure-android.info/).
* In choosing an icon for the Figurehead app, I come across the wonderful [The Digital Michelangelo Project](http://graphics.stanford.edu/projects/mich/). I take the liberty of using a meshed（figure)head of David as the icon of Figurehead, in homage of their work. I hope the authors would not mind.

[core]: https://pw4ever.github.io/tbnl/common/tbnl.core/docs/uberdoc.html "core API"
[mastermind]: https://pw4ever.github.io/tbnl/common/tbnl.core/docs/uberdoc.html "annotated source code of mastermind"
[cnc]: https://pw4ever.github.io/tbnl/host-side-tools/tbnl.cnc/docs/uberdoc.html "annotated source of C\&C"
[figurehead]: https://pw4ever.github.io/tbnl/guest-side-tools/tbnl.figurehead/docs/uberdoc.html "annotated source of figurehead"
[messenger]: https://pw4ever.github.io/tbnl/host-side-tools/tbnl.messenger/docs/uberdoc.html "annotated source of messenger"
