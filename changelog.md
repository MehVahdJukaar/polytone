Added Client light and Particle Emitters block modifier attributes

example:

"client_light": 15,
"particle_emitters": [
    {
        "x": "0.5+sin(TIME)",
        "y": "2",
        "z": "0.5+cos(TIME)",
        "chance": "1",
        "particle": "smoke",
        "dx": "0",
        "dy": "0",
        "dz": "0"
    }
]