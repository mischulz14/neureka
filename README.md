<h1 align="center"> <a href="https://gleethos.github.io/neureka/index.html">NEUREKA</a> </h1>

<h2 align="center"><b> A lightweight <br> platform independent <br> tensor library for the JVM </b></h2>

<p align="center">OpenCL accelerated nd-arrays / tensors for Java, Kotlin, Groovy, Scala, Jython, JRuby...</p>
 
 <!---
  - Visit [Neurekas homepage](https://gleethos.github.io/neureka/index.html) for more information!
  - Try out the latest release: [neureka.jar](https://github.com/Gleethos/neureka/raw/master/production/lib/neureka-0.18.0.jar)
 -->
 <!--- - [![HitCount](http://hits.dwyl.com/Gleethos/neureka.svg)](http://hits.dwyl.com/Gleethos/neureka) -->
 
| Current Build | Code Coverage | Version  | Code Quality | Licence | Size |
|:-------------:|:-------------:|:--------:|:------------:|:-------:|:----:|
| [![Build Status](https://circleci.com/gh/Gleethos/neureka.svg?branch=master&style=shield)](https://app.circleci.com/pipelines/github/Gleethos/neureka) | [![Codacy Badge](https://app.codacy.com/project/badge/Coverage/6bfd22ba9b8c410285b19e3d37f4fbc6)](https://www.codacy.com/gh/Gleethos/neureka/dashboard?utm_source=github.com&utm_medium=referral&utm_content=Gleethos/neureka&utm_campaign=Badge_Coverage) | [![GitHub version](https://badge.fury.io/gh/Gleethos%2Fneureka.svg)](https://github.com/Gleethos/neureka) ![Java Version](https://img.shields.io/static/v1.svg?label=Java&message=8%2B&color=blue) | [![Codacy Badge](https://api.codacy.com/project/badge/Grade/6bfd22ba9b8c410285b19e3d37f4fbc6)](https://www.codacy.com/manual/Gleethos/neureka?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Gleethos/neureka&amp;utm_campaign=Badge_Grade) | [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) |![Size](https://img.badgesize.io/Gleethos/neureka/master/production/lib/neureka-0.18.0.jar) |

---  

## :hammer_and_wrench: Features ##

<table>
<tr>
</tr>
<tr>
<td> 

- [tensors & nd-arrays](docs/markdown/tensors_or_nd-arrays.md)
- convolution
- broadcasting
- dynamic computation graph
- autograd
 
</td>
<td>

- flexible indexing and slicing
- seeding
- labeling
- jpg, png, idx support
- [highly extensible backend](docs/markdown/extending_neureka.md)

</td>
</tr>
</table>


### Take a quick look :eyes: ###

<table>
    <tr>
	<th>Impress me!</th>
	<th>Show me more</th>
	<th>Documentation</th>
    </tr> 
    <tr>
<td> 

- [ANN in 11 Lines of Code!](docs/markdown/impressive.md)

</td>
<td>

- [Neureka with Java](docs/markdown/java_example.md) :coffee:
- [Neureka with Groovy](docs/markdown/groovy_example.md) :star:
		
</td>
<td>

- [living<br>documentation](https://gleethos.github.io/neureka/showcase.html)
- [Javadocs](https://gleethos.github.io/neureka/jdocs/index.html) :book:

</td>
    </tr>
</table>


---

## :robot: Tech ##

### Dynamic Autograd : Recording the Computation-Graph ### 

Neureka trains your neural network using a computation graph recorder.

This is contrary to the approaches found in other frameworks such as TensorFlow, Theano, Caffe, and CNTK 
which require the definition of a computation graph ahead of time. 
This means a developer has to build a neural network structure which 
cannot change during runtime.   

Neureka, uses the recorded computation graph in order to apply a technique called reverse-mode auto-differentiation, 
which allows your network structure to change during runtime arbitrarily with zero lag or overhead.<br>
This powerful feature has been inspired by PyTorch:
 

- [Motivation](docs/markdown/motivation.md) :fire:
 
### Main-Package Overview ###
 
| Package | Description |
| ---- | --- |
| [**neureka**](src/main/java/neureka/README.md) | the root package containing the tensor class and the following sub-packages |
| [**neureka.devices**](src/main/java/neureka/devices/README.md) | a sub-package which enables cross platform acceleration (`OpenCLDevice`) and tensor persistence (`FileDevice`) |
| [**neureka.calculus**](src/main/java/neureka/calculus/README.md) | a sub-package containing collections of functions and the ability to create custom ones |
| [**neureka.optimization**](src/main/java/neureka/optimization/README.md) | a sub-package for weight-gradient optimization |
| [**neureka.autograd**](src/main/java/neureka/autograd/README.md) | the guts of Neurekas autograd system |
| [**neureka.backend**](src/main/java/neureka/backend/README.md) | the backend containing both a consistent API and a standard implementation  |
 
 
---
## Getting started with Apache Maven ##

```
<dependency>
  <groupId>com.github.gleethos</groupId>
  <artifactId>neureka</artifactId>
  <version>0.18.0</version>
</dependency>
```
---

## Getting started with Gradle ##
Groovy DSL:
```
implementation 'com.github.gleethos:neureka:0.18.0'
```
Kotlin DSL:
```
implementation("com.github.gleethos:neureka:0.18.0")
```
---

## Getting started with [![](https://jitpack.io/v/Gleethos/neureka.svg)](https://jitpack.io/#Gleethos/neureka) ##
**1. Add the JitPack url in your root `build.gradle` at the end of `repositories`**
```
allprojects {
	repositories {
		//...
		maven { url 'https://jitpack.io' }
	}
}
```
**2. Add Neureka as dependency**

...either by specifiying the version tag:
```
dependencies {
	implementation 'com.github.Gleethos:neureka:v0.18.0'
}
```
...or by using a custom commit hash instead:
```
dependencies {
	implementation 'com.github.Gleethos:neureka:8485bca'//Any commit hash...
}
```
---

## Getting started with Groovy Grape ##

```
@GrabResolver(name = 'jitpack.io', root = 'https://jitpack.io')
@Grab('com.github.Gleethos:neureka:v0.18.0')

import neureka.*
```

- [Start scripting with just a few terminal commands!](docs/markdown/cookbook/neureka_in_groovy_scripts.md)

---

## :rocket: Building from source ##

Execute the following:
```sh
 gradlew build
```

Tests:
```sh
 gradlew check
```

Jar file:
```sh
 gradlew jar
```

Min-jar file:
```sh
 gradlew proguard
```

---

## :mount_fuji: Dependencies ##

- JOCL 2.+ - (OpenCL binding)
- SLF4J 1.7.+ - (Logging API allowing for custom backends)

---

## :book: Documentation ###

- [Living Documentation](https://gleethos.github.io/neureka/showcase.html)
- [GitHub Wiki](https://github.com/Gleethos/neureka/wiki)
- [Java-Docs](https://gleethos.github.io/neureka/jdocs/index.html)

---

## :microscope: Tests & Specs :scroll: ###

- BDD & living documentation with Spock!
- Yes! A a browsable test suite. [Check it out!](https://gleethos.github.io/neureka/showcase.html)!

---

## :seedling: Development [![Commit activity 1 year](https://img.shields.io/github/commit-activity/y/Gleethos/neureka.svg?style=flat)]() [![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://GitHub.com/Gleethos/neureka/graphs/commit-activity) [![GitHub commits](https://img.shields.io/github/commits-since/Gleethos/neureka/v0.0.0.svg)](https://GitHub.com/Gleethos/neurka/commit/) ##

Want to contribute? Don't worry:

> **There is plenty of developer friendly highly readable [living documentation](https://gleethos.github.io/neureka/showcase.html) 
> to go through which explains the inner and outer workings of this project very well!**


If you want to dive right into it, start off by [extending the backend](docs/markdown/extending_neureka.md) 
for additional types of operations or data type support.

> Any feedback or contribution, even as simple as a typo fix, is always greatly appreciated!

---

## :memo: Todos - [![Issues](https://img.shields.io/github/issues-raw/Gleethos/neureka.svg?maxAge=25000)](https://github.com/Gleethos/neureka/issues)  ##

  - Make a wish! :)

---

## :balance_scale: License ##

- MIT -> **It's Free!** ... 
    
[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.png?v=103)](https://github.com/ellerbrock/open-source-badges/)

---

<!---

## CPU Only Test Coverage ##

[![Code Coverage](https://img.shields.io/codecov/c/github/gleethos/neureka)](https://codecov.io/github/gleethos/neureka)

## Stargazers over time

[![Stargazers over time](https://starchart.cc/Gleethos/neureka.svg)](https://starchart.cc/Gleethos/neureka)
-->      

[![Tweet](https://img.shields.io/twitter/url/https/github.com/Gleethos/neureka.svg?style=social)](https://twitter.com/intent/tweet?text=Check%20out%20Neureka!%20https://github.com/Gleethos/neureka)
