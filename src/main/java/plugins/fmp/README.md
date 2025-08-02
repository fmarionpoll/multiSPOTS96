# MultiSPOTS96 - Multi-Spot Analysis Plugin for Icy

## Overview

MultiSPOTS96 is a comprehensive image analysis plugin for the [Icy](http://icy.bioimageanalysis.org/) platform, designed for automated analysis of multi-spot experiments, particularly in behavioral studies involving flies (Drosophila) or other small organisms. The plugin provides tools for spot detection, fly tracking, measurement analysis, and data export.

## Key Features

### 🔍 **Multi-Spot Detection & Analysis**
- Automated detection and analysis of multiple spots in time-lapse sequences
- Support for up to 96 spots per experiment (hence the name)
- Advanced thresholding and image processing algorithms
- Real-time spot tracking and measurement

### 🦟 **Fly Tracking & Behavioral Analysis**
- Automated fly detection within defined cages/regions
- Position tracking over time
- Behavioral analysis including movement patterns
- Sleep/wake state detection
- Distance and velocity calculations

### 📊 **Comprehensive Measurements**
- Area measurements (sum, clean, difference)
- Fly presence detection
- Time-series analysis
- Statistical calculations
- Performance indices

### 📈 **Data Visualization & Export**
- Interactive charts and graphs
- Excel export functionality
- Multiple export formats
- Customizable data presentation

## Installation

### Prerequisites
- [Icy](http://icy.bioimageanalysis.org/) (version 2.0 or higher)
- Java 8 or higher
- Maven (for building from source)

### Installation Steps

1. **Download the Plugin**
   ```bash
   git clone https://github.com/your-repo/multiSPOTS96.git
   cd multiSPOTS96
   ```

2. **Build the Plugin**
   ```bash
   mvn clean install
   ```

3. **Install in Icy**
   - Copy the built JAR file to Icy's plugins directory
   - Restart Icy
   - The plugin will appear in the Plugins menu

## Usage Guide

### 1. **Browse & Load Experiments**

The **Browse** tab allows you to:
- **Create** new experiments
- **Open** existing experiments
- **Search** for experiment files
- **Navigate** between multiple experiments

**Key Features:**
- Optimized file loading for large datasets
- Support for multiple file formats (JPG, TIFF)
- Batch processing capabilities
- Progress tracking for large file sets

### 2. **Experiment Configuration**

The **Experiment** tab provides:

#### **Infos Tab**
- Define experiment metadata
- Set experiment parameters
- Configure analysis settings

#### **Filter Tab**
- Filter experiments based on descriptors
- Apply criteria-based selection
- Batch filtering operations

#### **Edit Tab**
- Edit experiment descriptors
- Modify experiment properties
- Update metadata

#### **Intervals Tab**
- View and edit time-lapse intervals
- Configure time series parameters
- Set analysis time windows

#### **Options Tab**
- Configure display options
- Set visualization parameters
- Customize analysis settings

#### **Correct Drift Tab**
- Correct image drift over time
- Align sequences for analysis
- Improve tracking accuracy

### 3. **Spot Detection & Management**

The **Spots** tab provides comprehensive spot analysis tools:

#### **Cages Tab**
- Create and manage experimental cages
- Define regions of interest
- Configure cage properties

#### **Detect Spots Tab**
- Automated spot detection
- Threshold-based detection
- Manual spot creation
- Spot validation tools

#### **Edit Tab**
- Edit spot positions
- Modify spot properties
- Adjust spot boundaries
- Validate spot data

#### **Infos Tab**
- Edit spot information
- Configure spot parameters
- Set measurement options

#### **Load/Save Tab**
- Save spot configurations
- Load previous spot data
- Export spot definitions
- Import spot templates

### 4. **Spot Measurements**

The **Measure spots** tab provides advanced measurement capabilities:

#### **Simple Threshold Tab**
- Basic threshold-based measurements
- Area calculations
- Intensity measurements
- Background subtraction

#### **Edit Tab**
- Edit measurement data
- Correct measurement errors
- Interpolate missing data
- Validate measurements

#### **Charts Tab**
- Interactive data visualization
- Time-series plots
- Statistical charts
- Custom graph creation

#### **Load/Save Tab**
- Save measurement data
- Load previous measurements
- Export measurement results
- Import measurement templates

### 5. **Data Export**

The **Export** tab provides comprehensive data export functionality:

#### **Common Options Tab**
- Configure export settings
- Set file formats
- Define export parameters
- Customize output options

#### **Spots Tab**
- Export spot measurements
- Generate Excel reports
- Create CSV files
- Export statistical data

## File Structure

The plugin expects the following directory structure for experiments:

```
experiment_root/
├── images/                    # Original image files (JPG/TIFF)
├── results/                   # Analysis results
│   ├── bin_1/                # Binned data
│   ├── bin_2/
│   └── ...
├── MS96_experiment.xml       # Experiment configuration
├── MS96_cages.xml           # Cage definitions
├── MS96_spotsMeasures.xml   # Spot measurements
└── MS96_fliesPositions.xml  # Fly tracking data
```

## Configuration Options

### Performance Settings

The plugin includes optimized settings for different use cases:

```java
// For maximum stability (recommended for large datasets)
private static final int BATCH_SIZE = 3;
private static final int MAX_CONCURRENT_THREADS = 1;
private static final int CACHE_SIZE = 25;

// For better performance (if system is stable)
private static final int BATCH_SIZE = 8;
private static final int MAX_CONCURRENT_THREADS = 3;
private static final int CACHE_SIZE = 75;
```

### Memory Management

- Automatic garbage collection between batches
- Configurable memory limits
- Optimized image processing
- Streaming data processing for large datasets

## Troubleshooting

### Common Issues

1. **Loading Stuck with Many Files**
   - Reduce batch size in settings
   - Increase memory allocation
   - Use sequential processing mode

2. **Memory Issues**
   - Enable memory cleanup options
   - Reduce concurrent processing
   - Process smaller batches

3. **Performance Problems**
   - Check system resources
   - Optimize image size
   - Use appropriate batch sizes

### Debug Mode

Enable detailed logging for troubleshooting:

```java
LOGGER.setLevel(Level.FINE);
```

## Advanced Features

### **Image Processing Pipeline**
- Multiple image transform options
- Background subtraction
- Noise reduction
- Image enhancement

### **Statistical Analysis**
- Time-series analysis
- Correlation studies
- Performance indices
- Behavioral metrics

### **Data Export Formats**
- Excel (.xlsx)
- CSV
- XML
- Custom formats

### **Batch Processing**
- Multi-experiment analysis
- Automated workflows
- Script-based processing
- Command-line interface

## API Reference

### Core Classes

- `Experiment`: Main experiment container
- `CagesArray`: Manages experimental cages
- `SpotsArray`: Handles spot detection and analysis
- `SpotMeasure`: Measurement calculations
- `FlyPosition`: Fly tracking data

### Key Methods

```java
// Load experiment
Experiment exp = new Experiment(directory);
exp.load_MS96_experiment();

// Detect spots
exp.load_MS96_spotsMeasures();

// Track flies
exp.load_MS96_fliesPositions();

// Export data
XLSExportMeasuresFromSpot exporter = new XLSExportMeasuresFromSpot();
exporter.exportToFile(filename, options);
```

## Contributing

### Development Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-repo/multiSPOTS96.git
   ```

2. **Import into IDE**
   - Import as Maven project
   - Configure Java 8+ SDK
   - Set up Icy dependencies

3. **Build and Test**
   ```bash
   mvn clean test
   mvn package
   ```

### Code Style

- Follow Java naming conventions
- Use comprehensive documentation
- Include unit tests
- Follow the existing code structure

## License

This project is licensed under the [MIT License](LICENSE).

## Support

### Documentation
- [User Manual](docs/user-manual.md)
- [API Documentation](docs/api.md)
- [Troubleshooting Guide](docs/troubleshooting.md)

### Community
- [GitHub Issues](https://github.com/your-repo/multiSPOTS96/issues)
- [Discussion Forum](https://github.com/your-repo/multiSPOTS96/discussions)
- [Wiki](https://github.com/your-repo/multiSPOTS96/wiki)

### Contact
- **Email**: support@multispots96.org
- **GitHub**: [@multispots96](https://github.com/multispots96)

## Acknowledgments

- **Icy Team**: For the excellent bioimage analysis platform
- **Contributors**: All those who have contributed to the project
- **Research Community**: For feedback and feature requests

## Version History

### v2.3.3 (Current)
- Optimized file loading performance
- Improved memory management
- Enhanced thread safety
- Better error handling

### v2.3.2
- Added advanced measurement features
- Improved Excel export
- Enhanced visualization tools

### v2.3.1
- Bug fixes and stability improvements
- Performance optimizations
- UI enhancements

---

**MultiSPOTS96** - Advanced multi-spot analysis for behavioral research 