# Bootiful Banners

This project builds on [Craig Burke](http://twitter.com/craigburke1)'s [_RIDICULOUS_ pull request](https://github.com/spring-projects/spring-boot/pull/4647) to convert images into color-coded ASCII

I've taken inspiration from [@cbornet](http://github.com/cbornet) to build a CLI that takes images and emits banner ASCII art. It uses Spring Boot to build the CLI since it has great support for parameters, and executable .jars already. It also supports stdin and stdout or `-Dbanner.input=..` and `-Dbanner.output=..` options:

```
cat ~/Desktop/doge.jpg |  ./bootiful-banner.jar > my-project/src/main/resources/banner.txt
```

Also, this project provides a REST API that takes image uploads and emits plain-text ASCII art.

```
curl -F "image=@/my/img.jpg"  -H "Content-Type: multipart/form-data"  bootiful-banners.cfapps.io/banner
```

Both projects are [here](https://github.com/joshlong/bootiful-banners) and Apache 2 licensed.

I'M NOT SORRY.
