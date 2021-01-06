def recentLTS = "2.176.4"
def configurations = [
    [ platform: "linux", jdk: "8", jenkins: null ],
    [ platform: "windows", jdk: "8", jenkins: recentLTS, javaLevel: "8" ],
    [ platform: "linux", jdk: "11", jenkins: recentLTS, javaLevel: "8" ],
    [ platform: "linux", jdk: "8", jenkins: "2.274" ],
]

buildPlugin(
    useAci: true,
    failFast: false,
    configurations: configurations
)
