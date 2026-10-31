# Field of view

**The question it answers:** with this lens on this camera, how much of the scene fits in the frame?

## When you'll use it

You're planning a shot before leaving home. Will the 24 mm take in the whole façade from across the
square? How far back do you need to stand to frame a group of ten? Which lens do you pack for the
mountain hut? The answer depends on two things only: the focal length and the size of the sensor.

## The maths

Picture a triangle. Its tip is the lens, its base is the sensor, and its height is the focal length.
Extend the two sides out past the lens, and you've got the angle of view. A longer focal length makes a
narrower triangle, and a bigger sensor makes a wider one.

```text
angle = 2 · atan(sensor size / (2 · f))
```

Put in the sensor's width, height or diagonal, and you'll get the horizontal, vertical or diagonal
angle. Lens manufacturers usually quote the diagonal, because it's the largest number.

The same similar triangles tell you what the frame covers at a distance D:

```text
framed width  = sensor width  · D / f
framed height = sensor height · D / f
```

### A worked example

A 24 mm lens at a subject 10 m away:

| | Horizontal | Vertical | Diagonal | Frame at 10 m |
|---|---|---|---|---|
| Full frame | 73.7° | 53.1° | 84.1° | 15.0 × 10.0 m |
| APS-C | 52.2° | 36.0° | 60.9° | 9.8 × 6.5 m |
| Micro 4/3 | 39.6° | 30.3° | 48.5° | 7.2 × 5.4 m |

Same lens, three different pictures. The drawing in the app shows this directly: it marks the frames
of smaller sensors inside yours, so you can see what the same lens would crop to.

## A useful habit

The framed width grows in step with the distance: twice as far, twice as wide. So you only need to
remember one case. A 50 mm on full frame frames a width of about 0.7 times the distance: at 10 m,
that's 7.2 m across.

Also note that moving back and changing lens are *not* the same thing. Both can give the same framing
of your subject, but the distance is what sets the perspective. A portrait at 85 mm from far back and
one at 24 mm from up close frame the same face and look like two different people. Choose your distance
first for the look, then the lens for the framing.

## What the app can't know

- **Close focus.** The formulas are accurate for subjects well beyond the focal length. Most lenses
  also narrow or widen slightly as they focus closer, a behaviour called focus breathing.
- **Fisheyes.** A fisheye lens doesn't follow this geometry, so don't trust the numbers for one.
- **Sensor crop in video.** Many cameras use only part of the sensor when shooting video. To account
  for that, choose the format that matches the crop rather than your camera.
