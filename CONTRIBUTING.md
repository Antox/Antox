# Contributing to Antox

Thank you for contributing to Antox. 
If you wish to discuss things with developers, you can join the #tox-dev channel on the [freenode IRC network](http://freenode.net)
You can join either via your preferred IRC client or by using 
[this web client](https://kiwiirc.com/client/irc.freenode.net?channel=#tox-dev).
Please note that developers are not always active and it may take a few hours to get a response

The following are a few guidelines for contribution:

[Code of Conduct (COC)](#coc)

[Creating an Issue](#creating-issue)
- [Questions](#questions)
- [Feature Requests and Ideas](#requests)
- [Reporting Issues and Bugs](#issues)

[Pull Requests](#pull-requests)
- [Rules for Code](#code-rules)
- [Commit Guidelines](#commits)
- [Submitting the Pull Request](#submitting-pr)

[The Antox Mug](#mug)

## <a name="coc"></a> Code of Conduct

Just be nice and respectful to one another. 
Trolling, flaming and personal attacks are not allowed on this repo, or anywhere on GitHub.
Posting of eggplant emojis :eggplant: is expressly forbidden.

## <a name="creating-issue"></a> Creating an issue

Remember to check the issue tracker before lodging your issue, as it may have already been answered.

### <a name="questions"></a> Questions

Any questions can be made either on the freenode IRC network in the #tox-dev channel, 
or by creating a [GitHub issue](https://github.com/Antox/Antox/issues/new).
Make sure your questions are in English and are easy to understand.
If you feel that your question has been answered remember to close the issue to clean up the issue tracker andso developers 
can help other people.

### <a name="features"></a> Feature Requests and Ideas 

Feature requests can be made through a [GitHub issue](https://github.com/Antox/Antox/issues/new).
If you can, also give ideas on how you would like the feature to be implemented.
Be sure to name your issue with the words feature or idea, instead of creating an issue like "why doesnt antox have x yet????".

### <a name="issues"></a> Reporting Issues and Bugs

If you have discovered something that may not be working correctly, or something that looks out of place,
you can create an issue using the [GitHub issue tracker](https://github.com/Antox/Antox/issues).
If you might be able to create a fix for this, then you can try [creating a pull request](#pull-requests).
Make sure that you outline the issue and how to reproduce it so that others can help find a fix.

## <a name="pull-requests"></a> Pull Requests

Pull requests are greatly appreciated as they help us make Antox better for everyone.
Before you start work on your PR, make sure that you comment on issues, and also create them, 
to let developers know that you are working on that. 
It wouldn't be good if you were to devote all your effort into something that is already in the process of being fixed.
There are a few guidelines to make sure the repo stays clean and tidy.

### <a name="code-rules"></a> Code Rules

The main programming language for Antox is [Scala](https://www.scala-lang.org/). 
Please make sure you are educated in this language before starting work on your PR.

Your work should be created using the automatic code formatting in Android Studio so that code can be read the same.
This can be done in Android Studio automatically before submitting your PR by going to `Code` then `Reformat Code`. 

Try to leave comments, where you can so that other developers can understand what the code does.
If the code still needs work then you can tag necessary areas via a `// TODO` or `// FIXME` comment.

If you need to add external libraries, try and find it on Maven first, as it reduces clutter in the repo.
You can search for a library using [Maven search](https://search.maven.org/).
If you are unable to find the library on maven, then you can either add a jar file to `app/libs/` or by 
adding it to `app/src/main/<language>`, making sure that package naming is still retained, 
eg `app/src/main/java/org/website/software/WebsiteSoftware.java`. Libraries that require Play Services are not allowed.

### <a name="commits"></a> Commit Guidelines

Your commits messages should be short, but descriptive.
Try and use proper grammer as it is proven to be harder to read text that is not gramatically correct.

Make sure that when you are commiting you are only changing what is described in the commit message.
If you just commited something and you want to change one little thing, try and use `git commit --amend` to add to the previous commit.
If you have already pushed to an upstream branch then you can use `git push --force` to overwrite the old commit.

Before creating the pull request, check to see if you haven't made too many commits.
You can squash other commits down by using `git rebase -i <commit hash>` and replacing `pick` with `squash` or `s` to squash the commit
into the previous one. You can also rename commits by replacing `pick` with `reword` or `r`.

### <a name="submitting-pr"></a> Submitting the Pull Request

When submitting a PR make sure you are clear on what has been changed. 
Avoid submitting overly large pull requests as these can be difficult to review,
usually it is a good idea to create seperate pull requests for seperate changes, 
as this speeds up the reviewing process.
Once you have created the PR avoid adding more commits unless a change has been suggested and you have mentioned 
that you are working on it. This means that we aren't going to be reviewing code that won't necessarily be merged.


------------

## <a name="mug"> The Antox Mug

Top contributors can also be eligible for the Antox Mug™. 
If you have over 500 commits and strive to make Tox the most secure and user friendly protocol there is,
then send a message to wiiaam on freenode.

<img src="https://files.wiiaam.com/antox_mug.png" alt="antox mug" />

(Import fees may apply. P&P not included. Picture described is not necessarily representative of what the mug may look like)
