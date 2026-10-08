from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field


class DocumentType(str, Enum):
    RESUME = "RESUME"
    SELF_INTRO = "SELF_INTRO"
    PORTFOLIO = "PORTFOLIO"


class SkillCategory(str, Enum):
    LANGUAGE = "LANGUAGE"
    FRAMEWORK = "FRAMEWORK"
    DATABASE = "DATABASE"
    CLOUD = "CLOUD"
    DEVOPS = "DEVOPS"
    OS = "OS"
    AI = "AI"
    LIBRARY = "LIBRARY"
    TOOL = "TOOL"
    OTHER = "OTHER"


class EducationType(str, Enum):
    SCHOOL = "SCHOOL"
    TRAINING = "TRAINING"

class CoverLetterSectionType(str, Enum):
    MOTIVATION = "MOTIVATION"
    JOB_COMPETENCY = "JOB_COMPETENCY"
    OTHER = "OTHER"
    UNCLASSIFIED = "UNCLASSIFIED"


class Evidence(BaseModel):
    text: str
    source: DocumentType
    page: Optional[int] = None
    paragraphIndex: Optional[int] = None


class BasicProfile(BaseModel):
    targetJob: Optional[str] = None
    careerLevel: Optional[str] = None
    major: Optional[str] = None
    evidence: List[Evidence] = Field(default_factory=list)


class Skill(BaseModel):
    name: str
    category: SkillCategory
    evidence: List[Evidence] = Field(default_factory=list)


class Project(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    role: Optional[str] = None
    teamSize: Optional[str] = None
    period: Optional[str] = None
    skills: List[str] = Field(default_factory=list)
    tasks: List[str] = Field(default_factory=list)
    achievements: List[str] = Field(default_factory=list)
    evidence: List[Evidence] = Field(default_factory=list)


class Experience(BaseModel):
    organization: Optional[str] = None
    role: Optional[str] = None
    period: Optional[str] = None
    tasks: List[str] = Field(default_factory=list)
    skills: List[str] = Field(default_factory=list)
    evidence: List[Evidence] = Field(default_factory=list)


class Education(BaseModel):
    type: EducationType
    institution: Optional[str] = None
    name: Optional[str] = None
    major: Optional[str] = None
    degree: Optional[str] = None
    period: Optional[str] = None
    status: Optional[str] = None
    evidence: List[Evidence] = Field(default_factory=list)


class Certificate(BaseModel):
    name: str
    status: Optional[str] = None
    issueDate: Optional[str] = None
    issuer: Optional[str] = None
    evidence: List[Evidence] = Field(default_factory=list)


class Competency(BaseModel):
    name: str
    description: Optional[str] = None
    evidence: List[Evidence] = Field(default_factory=list)

class CoverLetterSection(BaseModel):
    title: Optional[str] = None

    types: List[CoverLetterSectionType] = Field(default_factory=list)

    summary: Optional[str] = None

    evidence: List[Evidence] = Field(default_factory=list)

class DocumentAnalysisResult(BaseModel):
    basicProfile: BasicProfile
    skills: List[Skill] = Field(default_factory=list)
    projects: List[Project] = Field(default_factory=list)
    experiences: List[Experience] = Field(default_factory=list)
    education: List[Education] = Field(default_factory=list)
    certificates: List[Certificate] = Field(default_factory=list)
    competencies: List[Competency] = Field(default_factory=list)
    coverLetterSections: List[CoverLetterSection] = Field(default_factory=list)
    