#!/usr/bin/env python3
"""
NexusAI Audio Module - Générateur de Projet Python

Alternative Python au générateur Java pour créer l'arborescence
complète du projet à partir d'un fichier Markdown.

Usage:
    python project_generator.py <doc-file> <output-dir>

Exemple:
    python project_generator.py nexusai-audio-complete.md ~/projects/nexus-audio

Auteur: NexusAI Team
Version: 1.0.0
Date: 20 Octobre 2025
"""

import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from enum import Enum, auto


class FileType(Enum):
    """Types de fichiers supportés."""
    JAVA = auto()
    XML = auto()
    YAML = auto()
    SQL = auto()
    PROPERTIES = auto()
    MAKEFILE = auto()
    DOCKERFILE = auto()
    MARKDOWN = auto()
    SHELL = auto()
    PYTHON = auto()
    UNKNOWN = auto()


@dataclass
class FileInfo:
    """Information sur un fichier à générer."""
    relative_path: str
    content: str
    file_type: FileType


class ProjectGenerator:
    """Générateur de projet NexusAI Audio."""
    
    # Patterns de détection
    PATTERNS = {
        FileType.JAVA: re.compile(r'^//\s*(.+\.java)\s*$', re.MULTILINE),
        FileType.XML: re.compile(r'^<!--\s*(.+\.xml)\s*-->\s*$', re.MULTILINE),
        FileType.YAML: re.compile(r'^#\s*(.+\.(yml|yaml))\s*$', re.MULTILINE),
        FileType.SQL: re.compile(r'^--\s*(.+\.sql)\s*$', re.MULTILINE),
        FileType.PROPERTIES: re.compile(r'^#\s*(.+\.properties)\s*$', re.MULTILINE),
        FileType.MAKEFILE: re.compile(r'^#\s*(Makefile)\s*$', re.MULTILINE),
        FileType.DOCKERFILE: re.compile(r'^#\s*(Dockerfile)\s*$', re.MULTILINE),
        FileType.MARKDOWN: re.compile(r'^#\s*(.+\.md)\s*$', re.MULTILINE),
        FileType.SHELL: re.compile(r'^#\s*(.+\.sh)\s*$', re.MULTILINE),
        FileType.PYTHON: re.compile(r'^#\s*(.+\.py)\s*$', re.MULTILINE),
    }
    
    def __init__(self, doc_path: str, output_path: str):
        """
        Initialise le générateur.
        
        Args:
            doc_path: Chemin du fichier de documentation
            output_path: Chemin de sortie du projet
        """
        self.doc_path = Path(doc_path)
        self.output_path = Path(output_path)
        self.files: Dict[str, FileInfo] = {}
        
    def generate(self) -> None:
        """Génère le projet complet."""
        print("╔" + "=" * 58 + "╗")
        print("║  NexusAI Audio Module - Générateur de Projet (Python)  ║")
        print("╚" + "=" * 58 + "╝")
        print()
        
        # 1. Lire la documentation
        print("📖 Lecture du fichier de documentation...")
        documentation = self._read_file()
        
        # 2. Parser et extraire les fichiers
        print("🔍 Extraction des fichiers...")
        self._parse_documentation(documentation)
        print(f"   ✓ {len(self.files)} fichiers détectés")
        print()
        
        # 3. Créer l'arborescence
        print("📁 Génération de l'arborescence...")
        self._create_directories()
        
        # 4. Écrire les fichiers
        print("📝 Écriture des fichiers...")
        self._write_files()
        
        # 5. Afficher le résumé
        self._print_summary()
    
    def _read_file(self) -> str:
        """Lit le fichier de documentation."""
        try:
            return self.doc_path.read_text(encoding='utf-8')
        except FileNotFoundError:
            print(f"❌ Erreur: Fichier introuvable: {self.doc_path}")
            sys.exit(1)
        except Exception as e:
            print(f"❌ Erreur lors de la lecture: {e}")
            sys.exit(1)
    
    def _parse_documentation(self, documentation: str) -> None:
        """Parse la documentation et extrait les fichiers."""
        # Diviser en blocs de code
        blocks = documentation.split('```')
        
        for i, block in enumerate(blocks):
            if i % 2 == 0:  # Ignorer les blocs non-code
                continue
            
            lines = block.split('\n', 1)
            if len(lines) < 2:
                continue
            
            language = lines[0].strip().lower()
            content = lines[1] if len(lines) > 1 else ''
            
            # Parser selon le langage
            if language == 'java':
                self._parse_java_block(content)
            elif language in ('xml', 'pom'):
                self._parse_xml_block(content)
            elif language in ('yaml', 'yml'):
                self._parse_yaml_block(content)
            elif language == 'sql':
                self._parse_sql_block(content)
            elif language in ('bash', 'sh', 'shell'):
                self._parse_shell_block(content)
            elif language == 'python':
                self._parse_python_block(content)
            elif language in ('markdown', 'md'):
                self._parse_markdown_block(content)
            else:
                self._auto_detect(content)
    
    def _parse_java_block(self, content: str) -> None:
        """Parse un bloc de code Java."""
        match = self.PATTERNS[FileType.JAVA].search(content)
        
        if match:
            file_path = match.group(1).strip()
            file_content = content[match.end():].strip()
            self.files[file_path] = FileInfo(file_path, file_content, FileType.JAVA)
        else:
            # Essayer d'extraire depuis le package
            self._extract_from_package(content)
    
    def _extract_from_package(self, content: str) -> None:
        """Extrait un fichier Java depuis son package."""
        package_match = re.search(r'package\s+([a-z0-9.]+);', content)
        class_match = re.search(r'(public\s+)?(class|interface|enum)\s+(\w+)', content)
        
        if package_match and class_match:
            package_name = package_match.group(1)
            class_name = class_match.group(3)
            
            # Deviner le module
            module = self._guess_module(package_name)
            
            # Construire le chemin
            package_path = package_name.replace('.', '/')
            file_path = f"{module}/src/main/java/{package_path}/{class_name}.java"
            
            self.files[file_path] = FileInfo(file_path, content.strip(), FileType.JAVA)
    
    def _guess_module(self, package_name: str) -> str:
        """Devine le module à partir du package."""
        module_map = {
            '.api': 'nexus-audio-api',
            '.core': 'nexus-audio-core',
            '.stt': 'nexus-audio-stt',
            '.tts': 'nexus-audio-tts',
            '.webrtc': 'nexus-audio-webrtc',
            '.storage': 'nexus-audio-storage',
            '.emotion': 'nexus-audio-emotion',
            '.persistence': 'nexus-audio-persistence',
            '.tools': 'tools',
        }
        
        for key, module in module_map.items():
            if key in package_name:
                return module
        
        return 'nexus-audio-api'
    
    def _parse_xml_block(self, content: str) -> None:
        """Parse un bloc XML."""
        match = self.PATTERNS[FileType.XML].search(content)
        
        if match:
            file_path = match.group(1).strip()
            file_content = content[match.end():].strip()
            self.files[file_path] = FileInfo(file_path, file_content, FileType.XML)
        elif '<artifactId>' in content:
            self._extract_pom(content)
    
    def _extract_pom(self, content: str) -> None:
        """Extrait un fichier POM.xml."""
        artifact_match = re.search(r'<artifactId>([^<]+)</artifactId>', content)
        
        if artifact_match:
            artifact_id = artifact_match.group(1)
            
            if artifact_id == 'nexus-audio':
                file_path = 'pom.xml'
            else:
                file_path = f'{artifact_id}/pom.xml'
            
            self.files[file_path] = FileInfo(file_path, content.strip(), FileType.XML)
    
    def _parse_yaml_block(self, content: str) -> None:
        """Parse un bloc YAML."""
        match = self.PATTERNS[FileType.YAML].search(content)
        
        if match:
            file_path = match.group(1).strip()
            file_content = content[match.end():].strip()
            self.files[file_path] = FileInfo(file_path, file_content, FileType.YAML)
        elif 'spring:' in content:
            file_path = 'nexus-audio-api/src/main/resources/application.yml'
            self.files[file_path] = FileInfo(file_path, content.strip(), FileType.YAML)
        elif 'services:' in content:
            file_path = 'docker-compose.yml'
            self.files[file_path] = FileInfo(file_path, content.strip(), FileType.YAML)
    
    def _parse_sql_block(self, content: str) -> None:
        """Parse un bloc SQL."""
        match = self.PATTERNS[FileType.SQL].search(content)
        
        if match:
            file_path = match.group(1).strip()
            file_content = content[match.end():].strip()
            self.files[file_path] = FileInfo(file_path, file_content, FileType.SQL)
        elif 'CREATE TABLE' in content:
            file_path = 'nexus-audio-persistence/src/main/resources/db/migration/V1__create_voice_tables.sql'
            self.files[file_path] = FileInfo(file_path, content.strip(), FileType.SQL)
    
    def _parse_shell_block(self, content: str) -> None:
        """Parse un bloc shell."""
        if content.startswith('#!'):
            match = self.PATTERNS[FileType.SHELL].search(content)
            if match:
                file_path = match.group(1).strip()
                self.files[file_path] = FileInfo(file_path, content.strip(), FileType.SHELL)
    
    def _parse_python_block(self, content: str) -> None:
        """Parse un bloc Python."""
        match = self.PATTERNS[FileType.PYTHON].search(content)
        
        if match:
            file_path = match.group(1).strip()
            file_content = content[match.end():].strip()
            self.files[file_path] = FileInfo(file_path, file_content, FileType.PYTHON)
    
    def _parse_markdown_block(self, content: str) -> None:
        """Parse un bloc Markdown."""
        match = self.PATTERNS[FileType.MARKDOWN].search(content)
        
        if match:
            file_path = match.group(1).strip()
            file_content = content[match.end():].strip()
            self.files[file_path] = FileInfo(file_path, file_content, FileType.MARKDOWN)
        elif content.startswith('# '):
            self.files['README.md'] = FileInfo('README.md', content.strip(), FileType.MARKDOWN)
    
    def _auto_detect(self, content: str) -> None:
        """Détection automatique du type de fichier."""
        if '.PHONY:' in content or '@echo' in content:
            self.files['Makefile'] = FileInfo('Makefile', content.strip(), FileType.MAKEFILE)
        elif content.startswith('FROM '):
            self.files['Dockerfile'] = FileInfo('Dockerfile', content.strip(), FileType.DOCKERFILE)
        elif 'API_KEY=' in content or 'OPENAI' in content:
            self.files['.env.example'] = FileInfo('.env.example', content.strip(), FileType.PROPERTIES)
    
    def _create_directories(self) -> None:
        """Crée l'arborescence des dossiers."""
        directories = set()
        
        for file_info in self.files.values():
            parent_dir = Path(file_info.relative_path).parent
            if parent_dir != Path('.'):
                directories.add(parent_dir)
        
        for directory in directories:
            full_path = self.output_path / directory
            full_path.mkdir(parents=True, exist_ok=True)
        
        print(f"   ✓ {len(directories)} dossiers créés")
    
    def _write_files(self) -> None:
        """Écrit tous les fichiers."""
        written = 0
        
        for file_info in self.files.values():
            full_path = self.output_path / file_info.relative_path
            
            try:
                full_path.write_text(file_info.content, encoding='utf-8')
                written += 1
                
                if written % 10 == 0:
                    print(f"   ✓ {written}/{len(self.files)} fichiers écrits...")
                    
            except Exception as e:
                print(f"   ✗ Erreur écriture {file_info.relative_path}: {e}")
        
        print(f"   ✓ {written} fichiers écrits au total")
    
    def _print_summary(self) -> None:
        """Affiche le résumé."""
        print()
        print("=" * 60)
        print(" " * 23 + "RÉSUMÉ")
        print("=" * 60)
        print()
        
        # Compter par type
        type_counts = {}
        for file_info in self.files.values():
            type_counts[file_info.file_type] = type_counts.get(file_info.file_type, 0) + 1
        
        print("Fichiers générés par type :")
        for file_type, count in sorted(type_counts.items(), key=lambda x: x[1], reverse=True):
            print(f"  • {file_type.name:15s} : {count:3d} fichiers")
        
        print()
        print(f"Projet généré dans : {self.output_path}")
        print()
        print("=" * 60)
        print("✅ Génération terminée avec succès !")
        print("=" * 60)
        print()
        print("Prochaines étapes :")
        print(f"  1. cd {self.output_path}")
        print("  2. docker-compose up -d")
        print("  3. mvn clean install")
        print("  4. mvn spring-boot:run -pl nexus-audio-api")
        print()


def main():
    """Point d'entrée principal."""
    if len(sys.argv) < 3:
        print("Usage: python project_generator.py <doc-file> <output-dir>")
        print()
        print("Exemple:")
        print("  python project_generator.py nexusai-audio-complete.md ~/projects/nexus-audio")
        sys.exit(1)
    
    doc_path = sys.argv[1]
    output_path = sys.argv[2]
    
    try:
        generator = ProjectGenerator(doc_path, output_path)
        generator.generate()
    except Exception as e:
        print(f"❌ Erreur : {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
