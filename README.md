<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [TBNL: power tool for Android system hacking, in Clojure](#tbnl-power-tool-for-android-system-hacking-in-clojure)
  - [eye catcher](#eye-catcher)
  - [why?](#why)
  - [try it](#try-it)
    - [use cases](#use-cases)
  - [documentation](#documentation)
    - [annotated source in [Marginalia](https://github.com/gdeer81/marginalia)](#annotated-source-in-marginaliahttpsgithubcomgdeer81marginalia)
  - [permission](#permission)
  - [thanks](#thanks)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

TBNL: power tool for Android system hacking, in Clojure
=====
Copyright &copy; Wei Peng

One tool that rules all your [Android](http://www.android.com/) ([rooted](http://www.androidcentral.com/root)) devices/emulators, at scale, in [Clojure](http://clojure.org/).

TBNL (To Be Named Later) is a pun on the framework's open & exploratory nature, inspired by [Edi Weitz](http://weitz.de/)'s [TBNL](http://weitz.de/tbnl/).

eye catcher
-----

[![Clojure REPL](http://img.youtube.com/vi/jC-aaIewNkc/0.jpg)](http://youtu.be/jC-aaIewNkc)

[More demos on Wiki.](https://github.com/pw4ever/tbnl/wiki/demos)

why?
-----

* Android system tools ([`am`](https://developer.android.com/tools/help/adb.html#am), [`pm`](https://developer.android.com/tools/help/adb.html#pm), [`input`](http://stackoverflow.com/a/8483797), and more), on steroid, **all in one**, directly available to you under a **full-feature** (not some emulation) [remote Clojure REPL](https://www.youtube.com/watch?v=jC-aaIewNkc) **for all your (Android 4.3+/SDK 18+, rooted) Android devices and emulators**.
* No more 6G+ downloads and 30G+ caches of [AOSP](https://source.android.com/) for system hacking.
* **Instant** system hacking in Clojure with **internal** Android API fully exposed. 
* Life is too short for hacking Android in Java. Compare the source code of [our Clojure version](https://github.com/pw4ever/tbnl/blob/gh-pages/guest-side-tools/tbnl.figurehead/src/clojure/figurehead/api/view/input.clj) and [Android's Java version](https://github.com/android/platform_frameworks_base/blob/master/cmds/input/src/com/android/commands/input/Input.java) of `input`---our version is even more user friendly and fully programmable in the remote REPL **without** `adb`.
* Multiple [C\&Cs][cnc] and [figureheads][figurehead]; one [mastermind][mastermind]: Sharing the same extensible plug-in framework, batteries included, and still expanding.

try it
-----

First, make sure to [satisfy the build/runtime dependencies](https://github.com/pw4ever/tbnl/wiki/try-it-out#dependencies).

To try out TBNL, you have 2 options:

* [download & install it *without* the source code](https://github.com/pw4ever/tbnl/wiki/try-it-out#without-source): Easy and quick to get started; but may not have the latest version; does not support further development.
* [build & install it *with* the source code](https://github.com/pw4ever/tbnl/wiki/try-it-out#with-source): Longer to set up *the first time*; but easy to build against the latest version; support further development.

In both case, ensure that:
* Have a *sole* (in the sense that [`adb`](https://developer.android.com/tools/help/adb.html) can unambiguously find it) Android device/emulator with [Android SDK 18](https://developer.android.com/about/versions/android-4.3.html) running.
 
Then, you can [run the tools](https://github.com/pw4ever/tbnl/wiki/try-it-out#running-tbnl).

All tested on [Arch Linux](https://www.archlinux.org/).

### use cases

See [these canned use cases](https://github.com/pw4ever/tbnl/wiki/canned-use-cases) for ideas how TBNL can help explore the Android system.

documentation
-----

### annotated source in [Marginalia](https://github.com/gdeer81/marginalia)
The ultimate truth lies in the code.
* [core][core] 
* [mastermind][mastermind]
* [C\&C][cnc]
* [figurehead][figurehead] 

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
* [Dr. Feng Li](http://www.engr.iupui.edu/~fengli/) and [Dr. Xukai Zou](http://cs.iupui.edu/~xkzou/), who advise and support my PhD research that starts TBNL.
* The Clojure community.
  * In particular, [Alexander Yakushev](https://github.com/alexander-yakushev) and others in the small but welcoming family of [Clojure on Android](http://clojure-android.info/).

[core]: https://pw4ever.github.io/tbnl/common/tbnl.core/docs/uberdoc.html "core API"
[mastermind]: https://pw4ever.github.io/tbnl/common/tbnl.core/docs/uberdoc.html "annotated source code of mastermind"
[cnc]: https://pw4ever.github.io/tbnl/host-side-tools/tbnl.cnc/docs/uberdoc.html "annotated source of C\&C"
[figurehead]: https://pw4ever.github.io/tbnl/guest-side-tools/tbnl.figurehead/docs/uberdoc.html "annotated source of figurehead"
