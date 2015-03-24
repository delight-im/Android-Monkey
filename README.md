# Android Monkey

Library that generates pseudo-random streams of user input for Android apps

## Installation

 * Include one of the [JARs](JARs) in your `libs` folder
 * or
 * Copy the Java package to your project's source folder
 * or
 * Create a new library project from this repository and reference it in your project

## Usage

```
public class Config {

	/** Feature flag to enable or disable monkey tests in this app */
	public static boolean MONKEY_TESTS = true;

}

public class MyActivity extends Activity {

	private Monkey mMonkey;

	@Override
	protected void onResume() {
		super.onResume();

		if (Config.MONKEY_TESTS) {
			mMonkey = new Monkey(MyActivity.this);
			mMonkey.getTarget().setActivity(this);
			mMonkey.setInterval(250);
			// optional seed: mMonkey.setSequence(0);
			mMonkey.start();
		}
	}

	@Override
	protected void onPause() {
		if (Config.MONKEY_TESTS) {
			mMonkey.stop();
		}

		super.onPause();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (Config.MONKEY_TESTS) {
			// optionally exclude some views from receiving events
			mMonkey.exclude(someButton);
			mMonkey.exclude(someOtherView);
		}
	}

}
```

## Dependencies

 * Android 2.2+

## Contributing

All contributions are welcome! If you wish to contribute, please create an issue first so that your feature, problem or question can be discussed.

## License

```
Copyright 2015 delight.im <info@delight.im>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
