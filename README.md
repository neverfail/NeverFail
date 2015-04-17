NeverFail [![Release][release_img]](release_url) [![Build Status][build_status_img]](build_status_url) [![Issues][issues_img]](issues_url)
==========

NeverFail is a java jar executable to patch Articulate's QuizMaker's output.
It rewrite quiz.swf scoring function to update mark in case the quiz have been failed.
The new mark is picked at random every runtime and can be adjust in this code.
Main advantage of this method is that it doesn't produce .xml files and other data,
it's all on quiz.swf and invisible (file size don't change).

## Installation

1. Clone it: `git clone https://github.com/neverfail/NeverFail`
2. Ensure you have:
    1. Java JDK 1.7+
    2. Ant build executable
    3. Brain

## Usage

1. Simply run Ant build or release process.
    * Build project: `ant build`
    * Generate .jar (also build): `ant generate`
     
## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## History

* First decompilation.
* First successful patching of data.swf to set score.
* QuizCheater v1.0 that automatically set a static score (whether or not it is a valid score).
* QuizCheater v2.0 alwais change score for a random one, and ensure score us real.
* NeverFail v1.0 use QuizCheater and add a patch to only update score when user fail quiz.
* QuizCheater v3.0 support new type of quiz (only), static score update.
* QuizCheater v4.0 random score update with real score computing.
* QuizCheater v5.0 Support all quiz type, real score, lot of tuning available, act as a wrapper for data.swf.
* NeverFail v2.0 rework of NeverFail, it's not anymore using quizCheater. Only manipulating quiz.swf.

## Credits

Special thanks to the original decompiler maniac that allow us to work on
all these quiz projects.
Thanks to the QuizCheater's developers who gave a deep insight of the inner working.
All beta testers that provide helpful information.

Finally, NeverFail developers that worked days and nights to release in time.
This is the result of hard time of researching and bytecode manipulation,
only them could have done that. They deserve the biggest thank you.


## License

Read Licences.txt


[build_status_img]: https://travis-ci.org/neverfail/NeverFail.svg?branch=master
[build_status_url]: https://travis-ci.org/neverfail/NeverFail
[issues_img]: https://img.shields.io/github/issues/neverfail/NeverFail.svg
[issues_url]: https://github.com/neverfail/NeverFail/issues
[release_img]: https://img.shields.io/github/release/neverfail/NeverFail.svg
[release_url]: https://github.com/neverfail/NeverFail/releases/latest
