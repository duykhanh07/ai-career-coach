import React from "react";
import MDEditor from "@uiw/react-md-editor";

const CoverLetterPreview = ({ content }) => {
  return (
    <div className="py-4">
      <div data-color-mode="light">
        <MDEditor value={content} preview="preview" height={700} />
      </div>
    </div>
  );
};

export default CoverLetterPreview;