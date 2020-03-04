def recentLTS = "2.164.1"
def configurations = [
    [ platform: "windows", jdk: "8", jenkins: recentLTS, javaLevel: "8" ],
    [ platform: "windows", jdk: "11", jenkins: recentLTS, javaLevel: "8" ]
]

buildPlugin(
    forceAci: true,
    failFast: false,
    configurations: buildPlugin.recommendedConfigurations()
)
