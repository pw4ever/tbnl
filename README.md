<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [TBNL: power tool for Android system hacking, in Clojure](#tbnl-power-tool-for-android-system-hacking-in-clojure)
  - [blurb](#blurb)
  - [rationale](#rationale)
  - [try it out](#try-it-out)
    - [use cases](#use-cases)
    - [permission](#permission)
  - [Doc](#doc)
    - [annotated source in [Marginalia](https://github.com/gdeer81/marginalia)](#annotated-source-in-marginaliahttpsgithubcomgdeer81marginalia)
  - [Acknowledgments](#acknowledgments)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

TBNL: power tool for Android system hacking, in Clojure
=====
Copyright &copy; Wei Peng.
<write.to.peng.wei@gmail.com>
<https://github.com/pw4ever/tbnl>
[See the permissions.][permission]


One tool that rules all your [Android](http://www.android.com/) devices/emulators, at scale, in [Clojure](http://clojure.org/).

TBNL (To Be Named Later) is a pun on the framework's exploratory nature, inspired by [Edi Weitz](http://weitz.de/)'s [TBNL](http://weitz.de/tbnl/).

blurb
-----

* Android **system** programming in Clojure, with [REPL](http://tryclj.com/) everywhere.
* Easy deployment with [adb](https://developer.android.com/tools/help/adb.html) on real devices or emulators ([Genymotion](http://www.genymotion.com/) included).
* No 6G downloads or 30G+ builds of [AOSP](https://source.android.com/).
* Multiple [C\&Cs][cnc] and [figureheads][figurehead]; one [mastermind][mastermind].
* Extensible plug-in framework, batteries included, and expanding.

rationale
-----

[Please see the Wiki](https://github.com/pw4ever/tbnl/wiki/rationale).

try it out
-----

First, make sure to [satisfy the build/runtime dependencies](https://github.com/pw4ever/tbnl/wiki/try-it-out#dependencies).

To try out TBNL, you have 2 options:

* [download & install it *without* the source code](https://github.com/pw4ever/tbnl/wiki/try-it-out#dependencies): Easy and quick to get started; but may not have the latest version; does not support further development.
* [build & install it *with* the source code](https://github.com/pw4ever/tbnl/wiki/try-it-out#dependencies): Longer to set up *the first time*; but easy to build against the latest version; support further development.

In both case, ensure that:
* Have a *sole* (in the sense that [`adb`](https://developer.android.com/tools/help/adb.html) can unambiguously find it) Android device/emulator with [Android SDK 18](https://developer.android.com/about/versions/android-4.3.html) running.
 
Then, you can [run the tools](https://github.com/pw4ever/tbnl/wiki/try-it-out#running-tbnl). Checkout the [canned use cases]().

All tested on [Arch Linux](https://www.archlinux.org/).

### use cases

See [these canned use cases](https://github.com/pw4ever/tbnl/wiki/canned-use-cases) for ideas how TBNL can help explore the Android system.

### permission

I am no legal expert, and I am confused by [the plethora of Open Source licenses](https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses), so let me explain what I have in mind in plain words.

You are free to use and adapt this work in any way you want, with 2 overriding principles:
* Do not hold me or any contributors liable for your actions of using this software.
* Please do the courtesy of fair acknowledgments if you derive works from it (I'd appreciate if you could let me know your work, and even more if you contribute back).

Please consider:
* Reporting issues and sending in patches;
* Staring this project on GitHub;
* Citing this work if you publish a paper using it;
* Letting me know if you love it or how it can be improved.

Doc
-----

### annotated source in [Marginalia](https://github.com/gdeer81/marginalia)
* [core][core] 
* [mastermind][mastermind]
* [C\&C][cnc]
* [figurehead][figurehead] 

Acknowledgments
-----
* [Dr. Feng Li](http://www.engr.iupui.edu/~fengli/) and [Dr. Xukai Zou](http://cs.iupui.edu/~xkzou/), who advise my PhD research behind this work.
* The Clojure community.
  * In particular, [Alexander Yakushev](https://github.com/alexander-yakushev) and others in the small but welcoming family of [Clojure on Android](http://clojure-android.info/).

[core]: https://pw4ever.github.io/tbnl/common/tbnl.core/docs/uberdoc.html "core API"
[mastermind]: https://pw4ever.github.io/tbnl/common/tbnl.core/docs/uberdoc.html "annotated source code of mastermind"
[cnc]: https://pw4ever.github.io/tbnl/host-side-tools/tbnl.cnc/docs/uberdoc.html "annotated source of C\&C"
[figurehead]: https://pw4ever.github.io/tbnl/guest-side-tools/tbnl.figurehead/docs/uberdoc.html "annotated source of figurehead"

