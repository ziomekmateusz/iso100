# A photographer's guide to ISO100

Every tool in the app answers a question you will ask in the field: how long can I leave the shutter
open, how much of this scene will be sharp, when does the light get good? This guide explains what
each tool does, the maths behind it, and how to read the result, the way I would explain it to you
during a photography course.

You don't need to remember the formulas. The app does the sums. But once you see why the numbers come
out the way they do, you will start predicting them, and that is when you stop needing a calculator
for the easy cases.

## The tools

Read them in this order if you are starting out; each page builds a little on the one before.

1. [Equivalent exposure and ND filters](exposure.md): the same light, with different settings.
2. [Depth of field](depth-of-field.md): how much of the scene is sharp.
3. [Field of view](field-of-view.md): what fits in the frame.
4. [Stars without trails](stars.md): the longest exposure before stars become lines.
5. [Timelapse](timelapse.md): turning hours of shooting into seconds of video.
6. [Sun](sun.md): sunrise, sunset, twilights, golden hour and blue hour.
7. [Moon](moon.md): phase, illumination and where to find it.

## Four ideas you'll meet on every page

### The stop

A stop is a doubling or a halving of light. That's all it is. Go from 1/250 s to 1/125 s and you let
in twice the light: one stop. Go from ISO 100 to ISO 400 and the sensor signal is amplified four
times: two stops. Close the lens from f/5.6 to f/8 and you halve the light: one stop.

Because each of the three controls speaks the same unit, you can trade one for another. That trade is
the whole idea of the exposure triangle.

### Why f-numbers look so strange

The f-number N is the focal length divided by the diameter of the aperture. The light gathered depends
on the *area* of that opening, and area goes with the square of the diameter. So to double the light
you multiply the diameter by √2, about 1.4, and that's where the sequence comes from:

```text
f/1   f/1.4   f/2   f/2.8   f/4   f/5.6   f/8   f/11   f/16   f/22
```

Each number is the previous one times 1.4, and each step is one stop. This is also why you'll see
N² in the formulas: the f-number squared is what really measures the light.

### Printed values and real values

The numbers on your camera are rounded for humans. The true sequence of shutter speeds is powers of
two, so 1/125 s is really 1/128 s, and f/5.6 is really f/5.657.

The app keeps both. It shows you the familiar value, and it calculates with the exact one. That's why
ISO 100, f/8 and 1/125 s comes out as exactly EV 13 in the app, rather than the 12.97 you'd get with a
pocket calculator. The difference is tiny, but rounded numbers add up over a long chain of stops.

In Settings you choose how fine the dials are: full stops, half stops or third stops (the default,
and how most modern cameras work).

### Sensor size, crop factor and your camera

Several tools need to know your sensor, so the app asks you to choose a camera, or just a sensor
format if yours isn't among the 579 in the catalog.

The **crop factor** compares a sensor's diagonal with the 43.3 mm diagonal of full frame:

```text
crop factor = 43.27 mm / sensor diagonal
```

| Format | Size (mm) | Crop factor | Circle of confusion |
|---|---|---|---|
| Full frame | 36 × 24 | 1.0 | 0.029 mm |
| APS-C | 23.5 × 15.6 | 1.5 | 0.019 mm |
| APS-C (Canon) | 22.3 × 14.9 | 1.6 | 0.018 mm |
| Micro 4/3 | 17.3 × 13 | 2.0 | 0.014 mm |
| 1" | 13.2 × 8.8 | 2.7 | 0.011 mm |

A 24 mm lens on APS-C frames like a 36 mm on full frame: multiply by the crop factor to compare. The
lens itself doesn't change, you are simply using a smaller piece of its image.

The circle of confusion is explained in [Depth of field](depth-of-field.md).

## One last thing: red night

At night, your eyes take a good twenty minutes to adapt to the dark, and a single glance at a white
screen undoes it. The red night theme keeps the app readable without spoiling your night vision. You
can also let it switch on by itself when civil twilight ends, in Settings; it's off by default, and it
needs to know where you are.
