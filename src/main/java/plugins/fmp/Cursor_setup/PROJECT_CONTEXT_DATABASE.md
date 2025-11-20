# Project Context Database

This file stores comprehensive context information for each project in the workspace to enable more precise and contextual assistance.

## dbWave64 Project

### Basic Information
- **Project Type**: MFC C++ Application
- **IDE**: Visual Studio 2022
- **C++ Standard**: ISO C++ 14
- **Platform**: Windows (Win32/x64)
- **Architecture**: Legacy MFC application with modern optimizations

### Build Configurations
- **Debug|Win32**: Debug x86 build
- **Release|Win32**: Release x86 build
- **Target**: Windows executable (.exe)

### Key Technologies & Dependencies
- **Framework**: Microsoft Foundation Classes (MFC)
- **Database**: DAO (Data Access Objects)
- **Excel Integration**: COM automation for Excel
- **Hardware Support**: 
  - DataTranslation devices
  - CED (Cambridge Electronic Design) devices
  - Alligator devices
- **UI Components**: Custom GridCtrl, custom controls

### Project Structure
```
dbwave64/
├── dbWave64/                    # Main source code
│   ├── data_acquisition/        # Hardware interface modules
│   ├── dbView                  # display database in a report view
│   ├── dbView_Optimized        # display database in an optimized report view
│   ├── Controls/               # Custom UI controls
│   ├── GridCtrl/              # Custom grid component
│   ├── Excel/                 # Excel automation
│   ├── include/               # Third-party headers
│   └── res/                   # Resources
├── dbWave64_setup/            # Setup project
└── Cursor/                    # Documentation and guides
```

### Known Issues & Solutions
1. **Linker Warning LNK4075**: EDITANDCONTINUE vs INCREMENTAL conflict
   - **Cause**: `LinkIncremental=false` conflicts with `DebugInformationFormat=EditAndContinue`
   - **Solution**: Set `LinkIncremental=true` for Debug configurations
   - **File**: `dbwave64/dbWave64/dbWave64.vcxproj`

2. **Inheritance Issues**: ViewdbWave_Optimized class
   - **Problem**: Improper base class initialization
   - **Solution**: Added proper constructor initialization and DoDataExchange method
   - **Files**: `ViewdbWave_Optimized.h`, `ViewdbWave_Optimized.cpp`

### Development Environment
- **Compiler**: MSVC (Microsoft Visual C++)
- **Linker**: MSVC Linker
- **Debugger**: Visual Studio Debugger
- **Version Control**: Git
- **Build System**: MSBuild

### Common Build Issues
- **64-bit Migration**: Project originally 32-bit, being migrated to 64-bit
- **MFC Compatibility**: Using legacy MFC patterns with modern C++ features
- **Dependency Management**: Complex third-party library dependencies
- **Resource Management**: MFC resource files and COM objects

### Performance Considerations
- **Memory Management**: MFC automatic memory management
- **Threading**: Single-threaded MFC application
- **Database Access**: DAO for data persistence
- **UI Responsiveness**: MFC message pump architecture

---

## dataAcq Project

### Basic Information
- Project Type: Standalone MFC SDI app (acquisition-only)
- IDE: Visual Studio 2022
- C++ Standard: C++20
- Platform: Windows (Win32 now; x64 later)

### Purpose
- Decoupled data acquisition tool producing `.dat` files and a mailbox (`manifest.txt`, `diary.txt`) for dbWave64 to import later.

### Locations
- Solution: `C:\Users\fred\source\repos\dataAcq\dataAcq.sln`
- Mailbox helpers: `dataAcq\dataAcq\Mailbox.*`
- Acquisition code: `dataAcq\dataAcq\data_acquisition\*`
- Resources: `dataAcq\dataAcq\dataAcq.rc` includes `dbWave_src.rc` and `resource_dbwave_src.h`
- Context file: `C:\Users\fred\source\repos\dataAcq\Cursor\DATAACQ_SPLIT_PROGRESS_AND_CONTEXT.md`

### Notes
- Uses Data Translation OpenLayers; ensure 32-bit runtime present.
- Resource IDs imported from dbWave64; prune unused resources progressively.


## dbWave2 Project

### Basic Information
- **Project Type**: MFC C++ Application (Legacy)
- **IDE**: Visual Studio 2022
- **C++ Standard**: ISO C++ 14
- **Platform**: Windows (Win32/x64/ARM)
- **Status**: Fully functional legacy version, used as reference model for dbWave64 development

### Build Configurations
- **Debug|Win32**: Debug x86 build
- **Release|Win32**: Release x86 build

### Key Technologies
- **Framework**: Microsoft Foundation Classes (MFC)
- **Database**: DAO (Data Access Objects)
- **Hardware Support**: Same as dbWave64
- **Plugin Architecture**: Generic plugin system

### Role in Development
- **Reference Implementation**: Serves as the working model for dbWave64 development
- **Code Migration**: dbWave64 is being developed by migrating and optimizing code from dbWave2
- **Feature Validation**: New features in dbWave64 can be validated against dbWave2's proven functionality
- **Architecture Guide**: Provides architectural patterns and solutions that work in production

### Project Structure
```
dbWave2/
├── dbWave2/                    # Main source code
├── plugins/                    # Plugin system
│   ├── acquisition/           # Data acquisition plugins
│   ├── interfaces/            # Plugin interfaces
│   └── DataTranslationPlugin/ # Specific hardware plugin
└── dbWave2_setup/            # Setup project
```

---

## fmp (multiSPOTS96) Project

### Basic Information
- **Project Type**: Java Plugin for ICY
- **Language**: Java
- **IDE**: Any Java IDE (Eclipse, IntelliJ, etc.)
- **Platform**: Cross-platform (Windows, macOS, Linux)
- **Purpose**: Multi-well plate analysis for biological imaging

### Key Technologies
- **Framework**: ICY plugin architecture (built on ImageJ/Fiji but with specific quirks)
- **Image Processing**: Custom algorithms for spot detection
- **Data Analysis**: Statistical analysis of biological data
- **Excel Export**: Data export to Excel format
- **UI**: Swing-based user interface

### Project Structure
```
fmp/
├── multiSPOTS96/              # Main plugin code
│   ├── dlg/                   # Dialog classes
│   ├── experiment/            # Experiment management
│   ├── series/                # Image series processing
│   ├── tools/                 # Utility tools
│   └── MultiSPOTS96.java      # Main plugin class
```

### Development Environment
- **Java Version**: Java 8 or higher
- **Build System**: Ant or Maven (likely)
- **Dependencies**: ICY API (based on ImageJ/Fiji but with ICY-specific extensions)
- **Testing**: JUnit (likely)

### ICY-Specific Considerations
- **Plugin Architecture**: ICY has its own plugin system built on top of ImageJ/Fiji
- **API Differences**: May have ICY-specific APIs and conventions
- **UI Integration**: ICY-specific UI integration patterns
- **Data Handling**: ICY-specific data structures and formats

---

## General Development Context

### User Preferences
- **Documentation**: Prefers markdown files with ALL CAPS filenames
- **Code Style**: Clean, well-documented code
- **Problem Solving**: Systematic approach with detailed explanations
- **File Organization**: Structured documentation in dedicated folders

### Common Patterns
1. **Legacy Code Migration**: Moving from older to newer technologies
2. **Cross-Platform Compatibility**: Ensuring code works across different environments
3. **Performance Optimization**: Balancing functionality with performance
4. **Documentation**: Comprehensive documentation for complex systems

### Technical Expertise Areas
- **C++ Development**: MFC, modern C++, legacy code maintenance
- **Java Development**: ImageJ plugins, scientific computing
- **Windows Development**: Win32 API, COM, hardware integration
- **Database Systems**: DAO, data persistence
- **Build Systems**: MSBuild, Visual Studio project management

### Communication Style
- **Precise Answers**: Detailed technical explanations
- **Context-Aware**: References previous conversations and project history
- **Solution-Oriented**: Provides actionable solutions with explanations
- **Documentation-Focused**: Creates comprehensive documentation

---

## Update Log

- **2024-01-XX**: Initial creation with dbWave64, dbWave2, and fmp project contexts
- **2024-01-XX**: Added linker warning LNK4075 context and solution
- **2024-01-XX**: Added inheritance fix context for ViewdbWave_Optimized

---

*This file should be updated whenever new context information becomes available or when project configurations change.*
