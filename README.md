# Why another MediaPlayer?

I just recently found a video player app I used got bought and turned into ad infused spyware so this is an attempt to write a video player that hopefully does what I want and nothing more.

Right now the plan is having two Activities

- MainActivity : an UI to launch SAF to pick local file to play
- PlayerActivity : Actual player backed by ExoPlayer and handles View intent to open videos


